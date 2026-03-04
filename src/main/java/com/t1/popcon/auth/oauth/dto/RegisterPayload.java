package com.t1.popcon.auth.oauth.dto;

import com.t1.popcon.auth.oauth.service.OAuthProvider;

/**
 * OAuth 신규회원 임시 상태를 Redis에 저장하기 위한 Payload
 *
 * - OAuth 콜백에서 생성되어 Redis에 저장됨
 * - 본인인증/약관에서 registerToken으로 조회해서 이어서 사용
 */
public record RegisterPayload(
        OAuthProvider provider,
        String providerUserId,
        String email,
        String nickname,
        String name,
        String profileImageUrl,

        // 본인인증 완료 단계에서 채워질 수 있음(선택)
        String ciHash
) {
    public RegisterPayload withCiHash(String ciHash) {
        return new RegisterPayload(provider, providerUserId, email, nickname, name, profileImageUrl, ciHash);
    }
}
