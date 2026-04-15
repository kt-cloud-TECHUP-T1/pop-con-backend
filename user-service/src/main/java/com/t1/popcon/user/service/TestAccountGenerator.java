package com.t1.popcon.user.service;

import com.t1.popcon.common.auth.domain.TokenType;
import com.t1.popcon.common.auth.provider.TokenProvider;
import com.t1.popcon.common.encryption.EncryptionService;
import com.t1.popcon.user.billing.client.PortOneBillingClient;
import com.t1.popcon.user.billing.client.PortOneBillingClient.PortOneBillingRequest;
import com.t1.popcon.user.billing.client.PortOneBillingClient.PortOneBillingResponse;
import com.t1.popcon.user.billing.entity.UserBillingKey;
import com.t1.popcon.user.billing.repository.UserBillingKeyRepository;
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
                final int index = i;
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
                        CsvRow row = f.get();
                        if (row != null) {
                            writer.write(row.toCsv());
                            writer.newLine();
                        }
                    }
                    futures.clear();
                    writer.flush();
                }
            }
        } catch (ExecutionException e) {
            log.error("[TestAccount] CSV 작성 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                    List<Runnable> cancelledTasks = executor.shutdownNow();
                    log.warn("[TestAccount] 스레드 풀 종료 타임아웃. 취소된 작업 수: {}, 총 시도된 작업: {}", cancelledTasks.size(), count);
                    log.warn("[TestAccount] CSV 기록과 실제 DB 생성 수에 불일치가 발생했을 수 있습니다.");
                }
            } catch (InterruptedException e) {
                List<Runnable> cancelledTasks = executor.shutdownNow();
                log.warn("[TestAccount] 스레드 풀 종료 중 인터럽트 발생. 취소된 작업 수: {}", cancelledTasks.size());
                Thread.currentThread().interrupt();
            }
        }

        log.info("[TestAccount] 대량 계정 생성 완료. 파일 위치: {}", fileName);
        return fileName;
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

        } catch (Exception e) {
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
