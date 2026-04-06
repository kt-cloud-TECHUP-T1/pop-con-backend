package com.t1.popcon.auth.oauth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
/**
 * application.yml의 app.oauth 설정을 바인딩하는 클래스
 */
@ConfigurationProperties(prefix = "app.oauth")
@Validated
public record OAuthProperties(

        // 백엔드 기본 URL (redirect_uri 생성용)
        @NotBlank String baseUrl,

        // 콜백 path ( {provider} 치환 사용 )
        @NotBlank String redirectPath,

        // state TTL (Redis 저장 시간)
        long stateTtlSeconds,

        @Valid @NotNull Provider kakao,
        @Valid @NotNull Provider naver
) {

    /**
     * Provider 공통 설정값
     */
    public record Provider(
            @NotBlank String clientId,
            String clientSecret,
            @NotBlank String authorizeUrl,
            @NotBlank String tokenUrl,
            @NotBlank String userinfoUrl,
            @NotNull List<String> scopes
    ) {}
}