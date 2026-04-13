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
import com.t1.popcon.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.hibernate.exception.ConstraintViolationException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import com.t1.popcon.user.dto.UserCreateRequest;
import com.t1.popcon.user.dto.UserCreateResponse;
import org.springframework.beans.factory.annotation.Value;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final int MAX_NICKNAME_RETRIES = 5;
    private static final String NICKNAME_CONSTRAINT = "uk_users_nickname";

    private final UserRepository userRepository;
    private final EncryptionService encryptionService;
    private final BillingKeyService billingKeyService;

    @Value("${user.nickname.prefix:User}")
    private String nicknamePrefix;

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
     * 사용자 상세 정보 조회 (내부 서비스용)
     */
    @Transactional(readOnly = true)
    public UserInternalResponse getUserInternal(Long userId) {
        User user = getUserOrThrow(userId);
        return new UserInternalResponse(
                user.getId(),
                user.getEncryptedName(),
                user.getEncryptedPhoneNumber()
        );
    }

    @Transactional(readOnly = true)
    public TicketPurchaserProfileResponse getTicketPurchaserProfile(Long userId) {
        User user = getUserOrThrow(userId);
        Optional<BillingKeyInfoResponse> billingKeyInfo = billingKeyService.getDefaultBillingKeyInfo(userId);
        return new TicketPurchaserProfileResponse(
            user.getId(),
            encryptionService.decrypt(user.getEncryptedName()),
            encryptionService.decrypt(user.getEncryptedPhoneNumber()),
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
                        request.encryptedBirthDate(),
                        request.encryptedGender(),
                        request.encryptedNationality(),
                        currentNickname,
                        request.email(),
                        request.providerUserId()
                );
                case "NAVER" -> User.createUserWithNaver(
                        request.ciHash(),
                        request.encryptedName(),
                        request.encryptedPhoneNumber(),
                        request.encryptedBirthDate(),
                        request.encryptedGender(),
                        request.encryptedNationality(),
                        currentNickname,
                        request.email(),
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
}
