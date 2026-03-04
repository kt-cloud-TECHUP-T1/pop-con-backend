package com.t1.popcon.auth.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OAuth Token 응답의 "공통" 형태만 받는 DTO
 * - kakao/naver 모두 access_token, token_type, refresh_token, expires_in 등을 내려줌
 *
 * 주의) provider마다 field가 조금씩 다를 수 있어서
 *       공통 필드만 받고 나머지는 필요할 때 확장하는 방식 추천
 */
public record OAuthTokenResponse(
        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("token_type")
        String tokenType,

        @JsonProperty("refresh_token")
        String refreshToken,

        @JsonProperty("expires_in")
        Long expiresIn,

        // scope는 있을 수도/없을 수도
        String scope
) { }
