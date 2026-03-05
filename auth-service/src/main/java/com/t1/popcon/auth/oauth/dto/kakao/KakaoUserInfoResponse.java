package com.t1.popcon.auth.oauth.dto.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Kakao: https://kapi.kakao.com/v2/user/me 응답 구조
 * 공식 문서 기반 :contentReference[oaicite:0]{index=0}
 */
public record KakaoUserInfoResponse(
        Long id,

        @JsonProperty("kakao_account")
        KakaoAccount kakaoAccount,

        Properties properties
) {
    public record KakaoAccount(
            String email,
            Profile profile
    ) {
        public record Profile(
                String nickname,
                @JsonProperty("profile_image_url")
                String profileImageUrl
        ) { }
    }

    public record Properties(
            String nickname,
            @JsonProperty("profile_image")
            String profileImage
    ) { }
}