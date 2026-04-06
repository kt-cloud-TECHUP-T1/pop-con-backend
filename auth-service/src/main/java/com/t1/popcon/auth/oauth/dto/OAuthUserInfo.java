package com.t1.popcon.auth.oauth.dto;

/**
 * provider별 응답(JSON)을 우리 서비스에서 쓰기 좋게 "정규화"한 사용자 정보
 *
 * 최소 기준:
 * - providerUserId: 소셜에서 내려주는 고유 ID (우리 DB에서 unique로 들고갈 값)
 * - email: 동의 안하면 null일 수 있음
 * - nickname/name: 없을 수 있음
 * - profileImageUrl: 없을 수 있음
 */
public record OAuthUserInfo(
        String providerUserId,
        String email,
        String nickname,
        String name,
        String profileImageUrl
) { }