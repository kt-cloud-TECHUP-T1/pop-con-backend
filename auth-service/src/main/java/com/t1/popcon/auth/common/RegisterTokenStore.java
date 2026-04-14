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
     * registerToken을 원자적으로 조회하고 삭제합니다. (GETDEL)
     * 회원가입 완료 등 1회성 소모가 필요한 시점에 동시성 제어를 위해 사용합니다.
     */
    Optional<RegisterPayload> consume(String registerToken);

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
            String phoneHash,
            String encryptedNationality,
            long ttlSecondsToExtend
    );

    // registerToken 삭제 (가입 완료/로그인 시)
    void delete(String registerToken);
}