package com.t1.popcon.auth.common;

import java.util.Optional;

/**
 * 회원 가입 임시 상태(RegisterPayload) Redis 저장소 인터페이스
 */
public interface RegisterTokenStore {

    // registerToken 저장
    void save(String registerToken, RegisterPayload payload, long ttlSeconds);

    // registerToken 존재 여부 확인 (만료 구분용)
    boolean exists(String registerToken);

    // registerToken 조회
    Optional<RegisterPayload> find(String registerToken);

    /**
     * 본인인증 정보를 registerToken payload에 병합
     */
    void mergeIdentityVerification(
            String registerToken,
            String ciHash,
            String encryptedName,
            String encryptedGender,
            String encryptedBirthDate,
            String encryptedPhoneNumber,
            String encryptedNationality,
            long ttlSecondsToExtend
    );

    // registerToken 삭제 (가입 완료/로그인 시)
    void delete(String registerToken);
}