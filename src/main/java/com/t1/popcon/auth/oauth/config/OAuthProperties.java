package com.t1.popcon.auth.oauth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
/**
 * application.yml의 app.oauth 설정을 바인딩하는 클래스
 *
 * prefix = app.oauth
 *
 * provider별 설정을 분리하여 관리
 */
@ConfigurationProperties(prefix = "app.oauth")
public record OAuthProperties(

        // 백엔드 기본 URL (redirect_uri 생성용)
        String baseUrl,

        // 콜백 path ( {provider} 치환 사용 )
        String redirectPath,

        // state TTL (Redis 저장 시간)
        long stateTtlSeconds,

        Provider kakao,
        Provider naver
) {

    /**
     * Provider 공통 설정값
     */
    public record Provider(
            String clientId,
            String clientSecret,
            String authorizeUrl,
            String tokenUrl,
            String userinfoUrl,
            List<String> scopes
    ) {}
}
