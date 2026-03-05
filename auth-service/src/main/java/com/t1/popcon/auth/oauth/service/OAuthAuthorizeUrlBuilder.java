package com.t1.popcon.auth.oauth.service;

import com.t1.popcon.auth.oauth.config.OAuthProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.StringJoiner;

/**
 * OAuth 인가 URL 생성기
 *
 * GET /auth/oauth/{provider} 호출 시
 * provider 로그인 페이지로 redirect할 URL을 생성
 */
@Component
public class OAuthAuthorizeUrlBuilder {

    private final OAuthProperties props;

    public OAuthAuthorizeUrlBuilder(OAuthProperties props) {
        this.props = props;
    }

    /**
     * provider별 authorize URL 생성
     */
    public String build(OAuthProvider provider, String state) {

        OAuthProperties.Provider p = providerProps(provider);

        // redirect_uri 생성
        String redirectUri = props.baseUrl()
                + props.redirectPath().replace("{provider}", provider.lower());

        // scope 문자열 생성
        String scope = joinScopes(p.scopes());

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(p.authorizeUrl())
                .queryParam("response_type", "code")
                .queryParam("client_id", p.clientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state);

        if (!scope.isBlank()) {
            builder.queryParam("scope", scope);
        }

        return builder.build().toUriString();
    }

    private OAuthProperties.Provider providerProps(OAuthProvider provider) {
        return switch (provider) {
            case KAKAO -> props.kakao();
            case NAVER -> props.naver();
        };
    }

    private String joinScopes(java.util.List<String> scopes) {
        if (scopes == null || scopes.isEmpty()) return "";

        StringJoiner joiner = new StringJoiner(" ");
        for (String s : scopes) {
            if (s != null && !s.isBlank()) {
                joiner.add(s.trim());
            }
        }
        return joiner.toString();
    }
}