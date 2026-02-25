package com.t1.popcon.user.service;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.user.domain.Gender;
import com.t1.popcon.user.domain.User;
import com.t1.popcon.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    /**
     * User 생성 - 카카오
     */
    public User createUserWithKakao(
        String ciHash,
        LocalDateTime ciVerifiedAt,
        String name,
        String phone,
        LocalDate birthDate,
        Gender gender,
        String kakaoUserId
    ) {
        validateSocialId(kakaoUserId);
        User user = User.createUserWithKakao(ciHash, ciVerifiedAt, name, phone, birthDate, gender, kakaoUserId);
        return userRepository.save(user);
    }

    /**
     * User 생성 - 네이버
     */
    public User createUserWithNaver(
        String ciHash,
        LocalDateTime ciVerifiedAt,
        String name,
        String phone,
        LocalDate birthDate,
        Gender gender,
        String naverUserId
    ) {
        validateSocialId(naverUserId);
        User user = User.createUserWithNaver(ciHash, ciVerifiedAt, name, phone, birthDate, gender, naverUserId);
        return userRepository.save(user);
    }

    /**
     * 소셜 추가 연결 - 본인인증에서 CI 기반 기존 계정 확인 시
     */
    public void linkKakaoByCi(String ciHash, String kakaoUserId) {
        validateSocialId(kakaoUserId);

        User user = userRepository.findByCiHash(ciHash)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.connectKakao(kakaoUserId, LocalDateTime.now());
    }

    public void linkNaverByCi(String ciHash, String naverUserId) {
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