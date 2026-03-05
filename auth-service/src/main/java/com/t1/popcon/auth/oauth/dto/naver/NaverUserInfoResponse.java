package com.t1.popcon.auth.oauth.dto.naver;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Naver: https://openapi.naver.com/v1/nid/me 응답 구조
 * 공식 문서 기반 :contentReference[oaicite:2]{index=2}
 */
public record NaverUserInfoResponse(
        String resultcode,
        String message,
        Response response
) {
    public record Response(
            String id,
            String email,
            String name,
            String nickname,
            @JsonProperty("profile_image")
            String profileImage
    ) { }
}