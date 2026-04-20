package com.t1.popcon.user.service;

import com.t1.popcon.common.encryption.EncryptionService;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.user.billing.dto.BillingKeyInfoResponse;
import com.t1.popcon.user.billing.service.BillingKeyService;
import com.t1.popcon.user.domain.User;
import com.t1.popcon.user.dto.history.TicketPurchaserProfileResponse;
import com.t1.popcon.user.dto.UserLookupResponse;
import com.t1.popcon.user.dto.UserInternalResponse;
import com.t1.popcon.user.dto.UserProfileResponse;
import com.t1.popcon.user.dto.UserProfileUpdateResponse;
import com.t1.popcon.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import com.t1.popcon.user.dto.UserCreateRequest;
import com.t1.popcon.user.dto.UserCreateResponse;
import com.t1.popcon.user.dto.SuperLoginResponse;
import com.t1.popcon.common.auth.provider.TokenProvider;
import com.t1.popcon.common.auth.domain.TokenType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final int MAX_NICKNAME_RETRIES = 5;
    private static final String NICKNAME_CONSTRAINT = "uk_users_nickname";
    private static final String PHONE_HASH_CONSTRAINT = "uk_users_phone_hash";

    private static final String SUPER_USER_TOTAL_COUNT_KEY = "super_user_total_count";
    private static final String SUPER_USER_CURRENT_INDEX_KEY = "super_user_current_index";
    private static final long ONE_YEAR_MS = 365L * 24 * 60 * 60 * 1000;

    /** 닉네임 유효성 패턴: 한글·영문·숫자 2~20자, 언더바(_) 최대 1개 허용, 공백·기타 특수문자 불가 */
    private static final Pattern NICKNAME_PATTERN =
            Pattern.compile("^(?!.*_.*_)[가-힣a-zA-Z0-9_]{2,20}$");

    private final UserRepository userRepository;
    private final EncryptionService encryptionService;
    private final BillingKeyService billingKeyService;
    private final S3Service s3Service;
    private final TokenProvider tokenProvider;
    private final StringRedisTemplate redisTemplate;

    @Value("${user.nickname.prefix:User}")
    private String nicknamePrefix;

    /**
     * 시연용 슈퍼 계정 순차 로그인
     */
    @Transactional
    public SuperLoginResponse loginSuperAccount() {
        // 1. 전체 슈퍼 계정 수 확인
        String totalCountStr = redisTemplate.opsForValue().get(SUPER_USER_TOTAL_COUNT_KEY);
        long totalCount;
        
        try {
            if (totalCountStr == null || "0".equals(totalCountStr)) {
                totalCount = refreshSuperUserTotalCount();
            } else {
                totalCount = Long.parseLong(totalCountStr);
            }
        } catch (NumberFormatException e) {
            log.warn("[SuperLogin] Redis total_count 파싱 실패: {}, DB에서 재조회합니다.", totalCountStr);
            totalCount = refreshSuperUserTotalCount();
        }

        // 2. Redis INCR를 사용하여 다음 인덱스 결정 (순환)
        Long currentIndex = redisTemplate.opsForValue().increment(SUPER_USER_CURRENT_INDEX_KEY);
        if (currentIndex == null) currentIndex = 1L;

        long targetIndex = (currentIndex % totalCount);
        if (targetIndex == 0) targetIndex = totalCount;

        // 3. Super_{index} 닉네임 유저 조회 (Role.SUPER 조건 추가로 보안 강화)
        String nickname = "Super_" + targetIndex;
        User user = userRepository.findByNicknameAndRole(nickname, com.t1.popcon.user.domain.Role.SUPER)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, nickname + " 슈퍼 계정을 찾을 수 없습니다."));

        // 4. 긴 유효기간 액세스 토큰 생성 (리프레시는 생략)
        String userIdStr = String.valueOf(user.getId());
        String accessToken = tokenProvider.createToken(userIdStr, ONE_YEAR_MS, TokenType.ACCESS);

        return new SuperLoginResponse(
                user.getId(),
                accessToken,
                ONE_YEAR_MS / 1000
        );
    }

    private long refreshSuperUserTotalCount() {
        long dbCount = userRepository.countByRole(com.t1.popcon.user.domain.Role.SUPER);
        if (dbCount == 0) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND, "생성된 슈퍼 계정이 없습니다.");
        }
        redisTemplate.opsForValue().set(SUPER_USER_TOTAL_COUNT_KEY, String.valueOf(dbCount));
        return dbCount;
    }

    /**
     * 사용자 프로필 조회 (GET /users/me)
     * 암호화된 민감정보를 복호화하여 반환
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        User user = getUserOrThrow(userId);
        return UserProfileResponse.of(
                user,
                encryptionService.decrypt(user.getEncryptedName()),
                encryptionService.decrypt(user.getEncryptedPhoneNumber()),
                encryptionService.decrypt(user.getEncryptedBirthDate()),
                encryptionService.decrypt(user.getEncryptedGender())
        );
    }

    /**
     * 사용자 프로필 수정 (PATCH /users/me/profile)
     * - nickname, file, deleteImage 중 변경할 항목만 처리
     * - file과 deleteImage를 동시에 요청하면 예외
     * - S3 삭제는 DB 커밋 이후 afterCommit에서 실행 (트랜잭션 롤백 방지)
     */
    @Transactional
    public UserProfileUpdateResponse updateProfile(
            Long userId,
            String nickname,
            MultipartFile file,
            boolean deleteImage
    ) {
        // file과 deleteImage 동시 요청 불가
        if (file != null && !file.isEmpty() && deleteImage) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "이미지 업로드와 삭제를 동시에 요청할 수 없습니다.");
        }

        User user = getUserOrThrow(userId);

        // 닉네임 변경
        if (nickname != null && !nickname.isBlank()) {
            validateNickname(nickname);
            checkNicknameDuplicate(userId, nickname);
            user.updateNickname(nickname);
            try {
                // saveAndFlush로 커밋 전 DB 제약 위반을 트랜잭션 내에서 즉시 감지
                userRepository.saveAndFlush(user);
            } catch (DataIntegrityViolationException e) {
                if (isNicknameConflict(e)) {
                    throw new CustomException(ErrorCode.NICKNAME_DUPLICATED);
                }
                throw e;
            }
        }

        // 새 이미지 업로드: 업로드 먼저, 기존 이미지 삭제는 커밋 후 실행
        if (file != null && !file.isEmpty()) {
            String oldUrl = user.getProfileImageUrl();
            String newUrl = s3Service.uploadProfileImage(userId, file);
            user.updateProfileImageUrl(newUrl);
            scheduleS3Delete(oldUrl);
        } else if (deleteImage) {
            // 이미지 삭제만 요청: DB 업데이트 후 커밋 후 S3 삭제
            String oldUrl = user.getProfileImageUrl();
            user.updateProfileImageUrl(null);
            scheduleS3Delete(oldUrl);
        }

        return new UserProfileUpdateResponse(user.getNickname(), user.getProfileImageUrl());
    }

    /**
     * DB 커밋 이후 S3 이미지 삭제 예약
     * 트랜잭션 롤백 시 S3 삭제가 실행되지 않도록 afterCommit에서 처리
     */
    private void scheduleS3Delete(String imageUrl) {
        if (imageUrl == null) return;
        // 활성 트랜잭션이 없으면 afterCommit 콜백 등록 불가 → 즉시 삭제 시도
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            s3Service.deleteProfileImage(imageUrl);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                s3Service.deleteProfileImage(imageUrl);
            }
        });
    }

    /** 닉네임 형식 검증: 한글 2~10자 또는 영문/숫자 1~16자 */
    private void validateNickname(String nickname) {
        if (!NICKNAME_PATTERN.matcher(nickname).matches()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "한글·영문·숫자 혼합 2~20자만 가능합니다.");
        }
    }

    /** 닉네임 중복 검증 - 본인 닉네임은 제외 */
    private void checkNicknameDuplicate(Long userId, String nickname) {
        if (userRepository.existsByNicknameAndIdNot(nickname, userId)) {
            throw new CustomException(ErrorCode.NICKNAME_DUPLICATED);
        }
    }

    /**
     * 사용자 상세 정보 조회 (내부 서비스용)
     */
    @Transactional(readOnly = true)
    public UserInternalResponse getUserInternal(Long userId) {
        User user = getUserOrThrow(userId);
        return new UserInternalResponse(
                user.getId(),
                user.getEncryptedName(),
                user.getEncryptedPhoneNumber(),
                user.getCiHash()
        );
    }

    @Transactional(readOnly = true)
    public TicketPurchaserProfileResponse getTicketPurchaserProfile(Long userId) {
        User user = getUserOrThrow(userId);
        Optional<BillingKeyInfoResponse> billingKeyInfo = billingKeyService.getDefaultBillingKeyInfo(userId);
        return new TicketPurchaserProfileResponse(
            user.getId(),
            encryptionService.decrypt(user.getEncryptedName()),
            // 복호화된 전화번호를 하이픈 형식으로 포맷
            formatPhone(encryptionService.decrypt(user.getEncryptedPhoneNumber())),
            user.getEmail(),
            billingKeyInfo.map(BillingKeyInfoResponse::cardName).orElse(null),
            billingKeyInfo.map(BillingKeyInfoResponse::cardNumber).orElse(null)
        );
    }

    /**
     * 통합 회원 생성 (소셜 제공자 분기 처리)
     */
    public UserCreateResponse createUser(UserCreateRequest request) {
        if (request.provider() == null || request.provider().isBlank()) {
            throw new CustomException(ErrorCode.INVALID_PROVIDER);
        }

        for (int i = 0; i < MAX_NICKNAME_RETRIES; i++) {
            String currentNickname = generateUniqueNickname();

            User user = switch (request.provider().toUpperCase()) {
                case "KAKAO" -> User.createUserWithKakao(
                        request.ciHash(),
                        request.encryptedName(),
                        request.encryptedPhoneNumber(),
                        request.phoneHash(),
                        request.encryptedBirthDate(),
                        request.encryptedGender(),
                        request.encryptedNationality(),
                        currentNickname,
                        request.email(),
                        Boolean.TRUE.equals(request.isMarketingAgreed()),
                        request.providerUserId()
                );
                case "NAVER" -> User.createUserWithNaver(
                        request.ciHash(),
                        request.encryptedName(),
                        request.encryptedPhoneNumber(),
                        request.phoneHash(),
                        request.encryptedBirthDate(),
                        request.encryptedGender(),
                        request.encryptedNationality(),
                        currentNickname,
                        request.email(),
                        Boolean.TRUE.equals(request.isMarketingAgreed()),
                        request.providerUserId()
                );
                default -> throw new CustomException(ErrorCode.INVALID_PROVIDER);
            };

            try {
                User savedUser = userRepository.save(user);
                return UserCreateResponse.from(savedUser);
            } catch (DataIntegrityViolationException e) {
                if (isNicknameConflict(e)) {
                    if (i == MAX_NICKNAME_RETRIES - 1) {
                        throw e;
                    }
                    continue;
                }
                throw e;
            }
        }
        throw new CustomException(ErrorCode.ERROR_SYSTEM);
    }

    private boolean isNicknameConflict(DataIntegrityViolationException e) {
        Throwable cause = e.getRootCause();
        if (cause instanceof ConstraintViolationException hibernateEx) {
            return NICKNAME_CONSTRAINT.equals(hibernateEx.getConstraintName());
        }
        return e.getMessage() != null && e.getMessage().contains(NICKNAME_CONSTRAINT);
    }

    @Transactional(readOnly = true)
    public UserLookupResponse findBySocial(String provider, String providerUserId) {
        if (provider == null || provider.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        if (providerUserId == null || providerUserId.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        return switch (provider.toUpperCase()) {
            case "KAKAO" -> userRepository.findByKakaoUserId(providerUserId)
                    .map(user -> UserLookupResponse.found(user.getId()))
                    .orElseGet(UserLookupResponse::notFound);
            case "NAVER" -> userRepository.findByNaverUserId(providerUserId)
                    .map(user -> UserLookupResponse.found(user.getId()))
                    .orElseGet(UserLookupResponse::notFound);
            default -> throw new CustomException(ErrorCode.INVALID_PROVIDER);
        };
    }

    /**
     * CI 해시로 사용자 조회 (본인인증 완료 후 기존 회원 확인)
     */
    @Transactional(readOnly = true)
    public UserLookupResponse findByCiHash(String ciHash) {
        if (ciHash == null || ciHash.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        return userRepository.findByCiHash(ciHash)
                .map(user -> UserLookupResponse.found(user.getId()))
                .orElseGet(UserLookupResponse::notFound);
    }

    /**
     * CI 기반 소셜 계정 연결 (본인인증 완료 후 기존 회원이 소셜 로그인 연결)
     */
    @Transactional
    public void linkSocialByCi(String ciHash, String provider, String providerUserId) {
        if (ciHash == null || ciHash.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        if (provider == null || provider.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_PROVIDER);
        }

        validateSocialId(providerUserId);

        switch (provider.toUpperCase()) {
            case "KAKAO" -> linkKakaoByCi(ciHash, providerUserId);
            case "NAVER" -> linkNaverByCi(ciHash, providerUserId);
            default -> throw new CustomException(ErrorCode.INVALID_PROVIDER);
        }
    }

    /**
     * 소셜 추가 연결 - 카카오
     */
    private void linkKakaoByCi(String ciHash, String kakaoUserId) {
        validateSocialId(kakaoUserId);

        User user = userRepository.findByCiHash(ciHash)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.connectKakao(kakaoUserId, LocalDateTime.now());
    }

    /**
     * 소셜 추가 연결 - 네이버
     */
    private void linkNaverByCi(String ciHash, String naverUserId) {
        validateSocialId(naverUserId);

        User user = userRepository.findByCiHash(ciHash)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.connectNaver(naverUserId, LocalDateTime.now());
    }

    /**
     * 휴대폰 번호 변경 (본인인증 CI 검증 완료 후 auth-service에서 호출)
     * phoneHash로 타 계정 중복 여부 사전 확인 + DB 제약 위반 시에도 U004 반환
     */
    @Transactional
    public void updatePhone(Long userId, String encryptedPhone, String phoneHash) {
        if (encryptedPhone == null || encryptedPhone.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (phoneHash == null || phoneHash.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        User user = getUserOrThrow(userId);

        // 현재 사용자의 번호와 동일한지 확인
        if (phoneHash.equals(user.getPhoneHash())) {
            throw new CustomException(ErrorCode.SAME_PHONE_NUMBER);
        }

        // 동일 번호가 다른 계정에 이미 등록되어 있는지 사전 확인 (레이스 컨디션 보완은 아래 catch에서 처리)
        userRepository.findByPhoneHash(phoneHash)
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new CustomException(ErrorCode.PHONE_ALREADY_IN_USE);
                });

        user.updatePhoneNumber(encryptedPhone, phoneHash);

        try {
            // flush를 강제하여 레이스 컨디션 시 DB 제약 위반을 트랜잭션 내에서 잡음
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            if (isPhoneHashConflict(e)) {
                throw new CustomException(ErrorCode.PHONE_ALREADY_IN_USE);
            }
            throw e;
        }
    }

    private boolean isPhoneHashConflict(DataIntegrityViolationException e) {
        Throwable cause = e.getRootCause();
        if (cause instanceof ConstraintViolationException hibernateEx) {
            return PHONE_HASH_CONSTRAINT.equals(hibernateEx.getConstraintName());
        }
        return e.getMessage() != null && e.getMessage().contains(PHONE_HASH_CONSTRAINT);
    }

    @Transactional
    public void block(Long userId) {
        getUserOrThrow(userId).block();
    }

    @Transactional
    public void activate(Long userId) {
        getUserOrThrow(userId).activate();
    }

    @Transactional
    public void softDelete(Long userId, Long deleterId) {
        getUserOrThrow(userId).softDelete(deleterId);
    }

    private void validateSocialId(String socialId) {
        if (socialId == null || socialId.isBlank()) {
            throw new CustomException(ErrorCode.SOCIAL_INFO_MISSING);
        }
    }

    private String generateUniqueNickname() {
        String shortUuid = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return nicknamePrefix + "_" + shortUuid;
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    /** 전화번호를 010-XXXX-XXXX 형식으로 변환 */
    private static String formatPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }
        String trimmed = phone.trim();
        if (trimmed.contains("-")) {
            return trimmed;
        }
        if (trimmed.length() == 11 && trimmed.matches("\\d+")) {
            return trimmed.substring(0, 3) + "-" + trimmed.substring(3, 7) + "-" + trimmed.substring(7);
        }
        return trimmed;
    }
}
