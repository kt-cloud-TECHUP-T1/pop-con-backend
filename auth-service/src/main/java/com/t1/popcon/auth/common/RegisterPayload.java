package com.t1.popcon.auth.common;

import com.t1.popcon.auth.oauth.service.OAuthProvider;

/**
 * 가입 프로세스(소셜 로그인 -> 본인인증 -> 약관동의) 단계별 임시 상태를 저장하는 Payload
 * - OAuth 콜백에서 생성되어 Redis에 저장되며, registerToken을 키로 사용하여 단계별로 데이터를 완성함
 */
public record RegisterPayload(
        // OAuth 정보 (평문)
        OAuthProvider provider,
        String providerUserId,
        String email,
        String nickname,
        String name,
        String profileImageUrl,

        // 본인인증 정보 (암호화)
        String ciHash,                    // CI 해시 (단방향)
        String encryptedName,             // 이름 (양방향 암호화)
        String encryptedGender,           // 성별 (양방향 암호화)
        String encryptedBirthDate,        // 생년월일 (양방향 암호화)
        String encryptedPhoneNumber,      // 전화번호 (양방향 암호화)
        String phoneHash,                 // 전화번호 해시 (단방향, 중복 검증용)
        String encryptedNationality       // 내외국인 여부 (양방향)
) {

    /**
     * OAuth 콜백 단계에서 최초 생성
     */
    public static RegisterPayload fromOAuth(
            OAuthProvider provider,
            String providerUserId,
            String email,
            String nickname,
            String name,
            String profileImageUrl
    ) {
        return new RegisterPayload(
                provider, providerUserId, email, nickname, name, profileImageUrl,
                null, null, null, null, null, null, null
        );
    }

    /**
     * 본인인증 완료 후 기존 OAuth 정보에 인증 정보를 결합한 새로운 Payload 생성
     */
    public RegisterPayload withIdentityVerification(
            String ciHash,
            String encryptedName,
            String encryptedGender,
            String encryptedBirthDate,
            String encryptedPhoneNumber,
            String phoneHash,
            String encryptedNationality
    ) {
        return new RegisterPayload(
                provider, providerUserId, email, nickname, name, profileImageUrl,
                ciHash, encryptedName, encryptedGender, encryptedBirthDate, encryptedPhoneNumber, phoneHash, encryptedNationality
        );
    }
}