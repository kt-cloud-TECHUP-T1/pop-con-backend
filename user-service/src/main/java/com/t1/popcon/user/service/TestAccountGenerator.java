package com.t1.popcon.user.service;

import com.t1.popcon.common.auth.domain.TokenType;
import com.t1.popcon.common.auth.provider.TokenProvider;
import com.t1.popcon.common.encryption.EncryptionService;
import com.t1.popcon.user.billing.client.PortOneBillingClient;
import com.t1.popcon.user.billing.client.PortOneBillingClient.PortOneBillingRequest;
import com.t1.popcon.user.billing.client.PortOneBillingClient.PortOneBillingResponse;
import com.t1.popcon.user.billing.entity.UserBillingKey;
import com.t1.popcon.user.billing.repository.UserBillingKeyRepository;
import com.t1.popcon.user.domain.Role;
import com.t1.popcon.user.domain.User;
import com.t1.popcon.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestAccountGenerator {

    public static final String SUPER_USER_TOTAL_COUNT_KEY = "super_user_total_count";

    private final UserRepository userRepository;
    private final UserBillingKeyRepository billingKeyRepository;
    private final EncryptionService encryptionService;
    private final TokenProvider tokenProvider;
    private final StringRedisTemplate redisTemplate;
    private final PortOneBillingClient portOneBillingClient;

    private TestAccountGenerator self;

    @Autowired
    public void setSelf(@Lazy TestAccountGenerator self) {
        this.self = self;
    }

    @Value("${portone.api.secret}")
    private String portOneSecret;

    @Value("${portone.channel-key}")
    private String channelKey;

    @Value("${portone.test-card.number:4045770000000000}")
    private String testCardNumber;

    @Value("${portone.test-card.expiry-year:28}")
    private String testCardExpiryYear;

    @Value("${portone.test-card.expiry-month:12}")
    private String testCardExpiryMonth;

    @Value("${portone.test-card.birth:010101}")
    private String testCardBirth;

    @Value("${portone.test-card.password:12}")
    private String testCardPassword;

    private static final int BATCH_SIZE = 100;
    private static final long ONE_YEAR_MS = 365L * 24 * 60 * 60 * 1000;

    public String generateBulk(int count) throws InterruptedException, IOException {
        int startOffset = getStartOffset();
        log.info("[TestAccount] 시작 인덱스 확인: {} (이후 {}개 생성 예정)", startOffset, count);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "/tmp/load_test_accounts_" + timestamp + ".csv";
        File file = new File(fileName);

        // 스레드 풀 설정 (포트원 API 병렬 호출용)
        ExecutorService executor = Executors.newFixedThreadPool(20);
        AtomicInteger progress = new AtomicInteger(0);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
            // 헤더 작성
            writer.write("userId,accessToken,refreshToken,ciHash,phone,name,billingKeyId,cardName");
            writer.newLine();

            List<CompletableFuture<CsvRow>> futures = new ArrayList<>();

            for (int i = 1; i <= count; i++) {
                final int index = startOffset + i; // 자동 추적된 오프셋 적용
                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        return self.createSingleAccount(index);
                    } catch (Exception e) {
                        log.error("[TestAccount] 계정 생성 실패 - index={}: {}", index, e.getMessage());
                        return null;
                    }
                }, executor).thenApply(row -> {
                    int current = progress.incrementAndGet();
                    if (current % 100 == 0) log.info("[TestAccount] 진행 상황: {}/{}", current, count);
                    return row;
                }));

                // 일정 주기로 결과를 취합하여 CSV 작성 및 메모리 관리
                if (futures.size() >= BATCH_SIZE || i == count) {
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                    for (CompletableFuture<CsvRow> f : futures) {
                        try {
                            CsvRow row = f.get();
                            if (row != null) {
                                writer.write(row.toCsv());
                                writer.newLine();
                            }
                        } catch (Exception e) {
                            log.error("[TestAccount] 결과 처리 중 오류: {}", e.getMessage());
                        }
                    }
                    futures.clear();
                    writer.flush();
                }
            }
        } catch (Exception e) {
            log.error("[TestAccount] CSV 작성 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                    List<Runnable> cancelledTasks = executor.shutdownNow();
                    log.warn("[TestAccount] 스레드 풀 종료 타임아웃. 취소된 작업 수: {}, 총 시도된 작업: {}", cancelledTasks.size(), count);
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        log.info("[TestAccount] 대량 계정 생성 완료. 파일 위치: {}", fileName);
        return fileName;
    }

    public void generateSuperAccounts(int count) throws InterruptedException {
        int startOffset = getSuperStartOffset();
        log.info("[SuperAccount] 슈퍼 계정 생성 시작: 기존 {}개, 추가 {}개 예정", startOffset, count);

        ExecutorService executor = Executors.newFixedThreadPool(20);
        AtomicInteger successCount = new AtomicInteger(0);
        java.util.concurrent.ConcurrentLinkedQueue<String> errors = new java.util.concurrent.ConcurrentLinkedQueue<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 1; i <= count; i++) {
            final int index = startOffset + i;
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    self.createSingleSuperAccount(index);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    String errorMsg = String.format("index=%d, error=%s", index, e.getMessage());
                    errors.add(errorMsg);
                    log.error("[SuperAccount] 슈퍼 계정 생성 실패 - {}", errorMsg);
                }
            }, executor));

            if (futures.size() >= 20 || i == count) {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                futures.clear();
            }
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        // 생성 완료 후 전체 슈퍼 계정 수 Redis 업데이트
        long totalCount = userRepository.countByRole(Role.SUPER);
        redisTemplate.opsForValue().set(SUPER_USER_TOTAL_COUNT_KEY, String.valueOf(totalCount));
        
        log.info("[SuperAccount] 슈퍼 계정 생성 작업 종료. 성공: {}, 실패: {}, 총 DB 계정: {}", 
                 successCount.get(), errors.size(), totalCount);
        
        if (!errors.isEmpty()) {
            log.warn("[SuperAccount] 생성 중 발생한 오류 목록: {}", String.join(" | ", errors));
            throw new IllegalStateException(String.format("슈퍼 계정 생성 부분 실패: %d건 실패 / %d건 시도", errors.size(), count));
        }
    }

    private int getSuperStartOffset() {
        return userRepository.findFirstByNicknameStartingWithOrderByIdDesc("Super_")
                .map(user -> {
                    try {
                        String nickname = user.getNickname();
                        return Integer.parseInt(nickname.substring(6)); // "Super_" 이후 숫자 추출
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .orElse(0);
    }

    /**
     * 비트랜잭션 환경에서 유저 저장과 빌링키 등록을 순차적으로 호출 (외부 API 호출 시 커넥션 점유 방지)
     */
    public void createSingleSuperAccount(int index) {
        String nickname = "Super_" + index;
        if (userRepository.existsByNickname(nickname)) {
            return;
        }

        // 1. 유저 저장 (Transactional)
        User user = self.saveSuperUser(index, nickname);

        // 2. 안티매크로 점수 설정 (Redis)
        setupAntiMacroScore(user.getId());

        // 3. 빌링키 발급 및 저장 (외부 API + Transactional)
        self.registerBillingKeyForSuperUser(user);
    }

    @Transactional
    public User saveSuperUser(int index, String nickname) {
        String rawName = "슈퍼테스터_" + index;
        String rawPhone = String.format("010-9999-%04d", index % 10000);
        String ci = "super_ci_" + index;

        User user = User.createSuperUser(
                encryptionService.generateHash(ci),
                encryptionService.encrypt(rawName),
                encryptionService.encrypt(rawPhone),
                encryptionService.generateHash(rawPhone),
                encryptionService.encrypt("1990-01-01"),
                encryptionService.encrypt("M"),
                encryptionService.encrypt("KOREA"),
                nickname,
                "super" + index + "@popcon.store"
        );
        return userRepository.save(user);
    }

    private void setupAntiMacroScore(Long userId) {
        String redisKey = "score:" + userId;
        // Redis는 트랜잭션 관리가 필요 없으므로 즉시 실행
        redisTemplate.opsForHash().put(redisKey, "total", "25");
    }

    /**
     * 외부 API 호출 후 결과를 DB에 저장 (REQUIRES_NEW로 별도 트랜잭션 처리)
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void registerBillingKeyForSuperUser(User user) {
        try {
            PortOneBillingResponse response = portOneBillingClient.issueBillingKey(
                    "PortOne " + portOneSecret,
                    new PortOneBillingRequest(
                            channelKey,
                            new PortOneBillingRequest.Customer(String.valueOf(user.getId())),
                            new PortOneBillingRequest.Method(
                                    new PortOneBillingRequest.Method.Card(
                                            new PortOneBillingRequest.Method.Card.Credential(
                                                    testCardNumber, testCardExpiryYear, testCardExpiryMonth, testCardBirth, testCardPassword
                                            )
                                    )
                            )
                    )
            );
            
            String billingKeyId = response.billingKeyInfo().billingKey();
            // 방어 로직: channels 리스트가 비어있을 경우 기본값 사용
            String cardName = "KCP 테스트 카드";
            if (response.billingKeyInfo().channels() != null && !response.billingKeyInfo().channels().isEmpty()) {
                cardName = response.billingKeyInfo().channels().get(0).name();
            }

            UserBillingKey userBillingKey = UserBillingKey.builder()
                    .user(user)
                    .customerUid(billingKeyId)
                    .pgProvider("KCP_V2")
                    .cardName(cardName)
                    .cardNumber(testCardNumber)
                    .isDefault(true)
                    .build();
            billingKeyRepository.save(userBillingKey);
        } catch (Exception e) {
            log.warn("[SuperAccount] 빌링키 발급 실패 (유저 ID: {}): {}", user.getId(), e.getMessage());
            // 시연용 계정이므로 빌링키 발급에 실패해도 로그만 남기고 유저는 유지
        }
    }

    private int getStartOffset() {
        return userRepository.findFirstByNicknameStartingWithOrderByIdDesc("Tester_")
                .map(user -> {
                    try {
                        String nickname = user.getNickname();
                        return Integer.parseInt(nickname.substring(7)); // "Tester_" 이후 숫자 추출
                    } catch (Exception e) {
                        log.warn("[TestAccount] 마지막 인덱스 파싱 실패 (nickname: {}), 0부터 시작합니다.", user.getNickname());
                        return 0;
                    }
                })
                .orElse(0); // 데이터가 없으면 0 반환
    }

    @Transactional
    public CsvRow createSingleAccount(int index) {
        // 1. 더미 데이터 생성 및 암호화
        String rawName = "테스터_" + index;
        String rawPhone = String.format("010-%04d-%04d", (index / 10000) % 10000, index % 10000);
        String ci = "test_ci_" + index;

        String encryptedName = encryptionService.encrypt(rawName);
        String encryptedPhone = encryptionService.encrypt(rawPhone);
        String phoneHash = encryptionService.generateHash(rawPhone);
        String ciHash = encryptionService.generateHash(ci);

        // 2. 유저 저장 (Social Login 연동 상태 재현)
        User user = User.createUserWithKakao(
                ciHash, encryptedName, encryptedPhone, phoneHash,
                encryptionService.encrypt("1990-01-01"),
                encryptionService.encrypt("M"),
                encryptionService.encrypt("KOREA"),
                "Tester_" + index,
                "test" + index + "@example.com",
                true,
                "test_kakao_" + (100000 + index) // 10만번대부터 소셜 ID 부여
        );
        userRepository.save(user);

        // 3. 안티매크로 점수 0점 설정 (퀴즈 면제)
        // Redis는 트랜잭션 관리가 되지 않으므로 DB 커밋 후에만 실행되도록 동기화 등록
        String redisKey = "score:" + user.getId();
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    redisTemplate.opsForHash().put(redisKey, "total", "0");
                }
            });
        } else {
            redisTemplate.opsForHash().put(redisKey, "total", "0");
        }

        // 4. 포트원 빌링키 발급 API 호출
        String billingKeyId = "";
        String cardName = "KCP 빌링키 테스트";

        try {
            PortOneBillingResponse response = portOneBillingClient.issueBillingKey(
                    "PortOne " + portOneSecret,
                    new PortOneBillingRequest(
                            channelKey,
                            new PortOneBillingRequest.Customer(String.valueOf(user.getId())),
                            new PortOneBillingRequest.Method(
                                    new PortOneBillingRequest.Method.Card(
                                            new PortOneBillingRequest.Method.Card.Credential(
                                                    testCardNumber, testCardExpiryYear, testCardExpiryMonth, testCardBirth, testCardPassword
                                            )
                                    )
                            )
                    )
            );
            billingKeyId = response.billingKeyInfo().billingKey();
            if (!response.billingKeyInfo().channels().isEmpty()) {
                cardName = response.billingKeyInfo().channels().get(0).name();
            }

            // 5. 빌링키 엔티티 저장
            UserBillingKey userBillingKey = UserBillingKey.builder()
                    .user(user)
                    .customerUid(billingKeyId)
                    .pgProvider("KCP_V2")
                    .cardName(cardName)
                    .cardNumber(testCardNumber)
                    .isDefault(true)
                    .build();
            billingKeyRepository.save(userBillingKey);

        } catch (feign.FeignException e) {
            log.warn("[TestAccount] 빌링키 발급 실패 (유저 ID: {}): {}", user.getId(), e.getMessage());
            // 빌링키 발급 실패해도 유저는 생성됨 (경매 참여는 불가)
        }

        // 6. 긴 유효시간 토큰 생성
        String userIdStr = String.valueOf(user.getId());
        String accessToken = tokenProvider.createToken(userIdStr, ONE_YEAR_MS, TokenType.ACCESS);
        String refreshToken = tokenProvider.createToken(userIdStr, ONE_YEAR_MS, TokenType.REFRESH);

        return new CsvRow(
                userIdStr, accessToken, refreshToken, ciHash, rawPhone, rawName, billingKeyId, cardName
        );
    }

    private record CsvRow(
            String userId, String accessToken, String refreshToken, String ciHash,
            String phone, String name, String billingKeyId, String cardName
    ) {
        public String toCsv() {
            return String.format("%s,%s,%s,%s,%s,%s,%s,%s",
                    userId, accessToken, refreshToken, ciHash, phone, name, billingKeyId, cardName);
        }
    }
}
