package com.t1.popcon.user.service;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.user.domain.User;
import com.t1.popcon.user.dto.UserLookupResponse;
import com.t1.popcon.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import com.t1.popcon.user.dto.UserCreateRequest;
import com.t1.popcon.user.dto.UserCreateResponse;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    /**
     * 통합 회원 생성 (소셜 제공자 분기 처리)
     */
    public UserCreateResponse createUser(UserCreateRequest request) {
        if (request.provider() == null || request.provider().isBlank()) {
            throw new CustomException(ErrorCode.INVALID_PROVIDER);
        }

        User user = switch (request.provider().toUpperCase()) {
            case "KAKAO" -> User.createUserWithKakao(
                    request.ciHash(),
                    request.encryptedName(),
                    request.encryptedPhoneNumber(),
                    request.encryptedBirthDate(),
                    request.encryptedGender(),
                    request.encryptedNationality(),
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
                    request.email(),
                    request.providerUserId()
            );
            default -> throw new CustomException(ErrorCode.INVALID_PROVIDER);
        };

        User savedUser = userRepository.save(user);
        return UserCreateResponse.from(savedUser);
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

    public void block(Long userId) {
        getUserOrThrow(userId).block();
    }

    public void activate(Long userId) {
        getUserOrThrow(userId).activate();
    }

    public void softDelete(Long userId, Long deleterId) {
        getUserOrThrow(userId).softDelete(deleterId);
    }

    private void validateSocialId(String socialId) {
        if (socialId == null || socialId.isBlank()) {
            throw new CustomException(ErrorCode.SOCIAL_INFO_MISSING);
        }
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}