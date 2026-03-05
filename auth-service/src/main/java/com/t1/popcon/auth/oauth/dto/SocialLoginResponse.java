package com.t1.popcon.auth.oauth.dto;

/**
 * OAuth 콜백 최종 응답
 * - 기존회원: access/refresh + userId
 * - 신규회원: registerToken + nextStep
 */
public record SocialLoginResponse(
        boolean isNewUser,

        // 기존 회원
        Long userId,
        String accessToken,
        String refreshToken,

        // 신규 회원
        String registerToken,
        String nextStep
) {
    public static SocialLoginResponse existing(Long userId, String accessToken, String refreshToken) {
        return new SocialLoginResponse(false, userId, accessToken, refreshToken, null, null);
    }

    public static SocialLoginResponse newUser(String registerToken, String nextStep) {
        return new SocialLoginResponse(true, null, null, null, registerToken, nextStep);
    }
}