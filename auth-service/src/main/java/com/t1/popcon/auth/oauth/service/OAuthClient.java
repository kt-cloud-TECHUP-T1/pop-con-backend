package com.t1.popcon.auth.oauth.service;

import com.t1.popcon.auth.oauth.config.OAuthProperties;
import com.t1.popcon.auth.oauth.dto.OAuthTokenResponse;
import com.t1.popcon.auth.oauth.dto.OAuthUserInfo;
import com.t1.popcon.auth.oauth.dto.kakao.KakaoUserInfoResponse;
import com.t1.popcon.auth.oauth.dto.naver.NaverUserInfoResponse;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * 외부 OAuth Provider API 호출 전용 컴포넌트
 *
 * 역할 분리 이유:
 * - Service는 "비즈니스 흐름"에 집중 (state 검증, 분기 등)
 * - Client는 "외부 HTTP 호출"에 집중 (token/userinfo)
 */
@Component
public class OAuthClient {

    private final OAuthProperties props;
    private final RestClient restClient;

    public OAuthClient(OAuthProperties props, RestClient.Builder builder) {
        this.props = props;
        this.restClient = builder.build();
    }

    /**
     * code를 access_token으로 교환
     *
     * Kakao: /oauth/token (POST, x-www-form-urlencoded) :contentReference[oaicite:4]{index=4}
     * Naver: /oauth2.0/token (GET/POST 가능, 보통 GET 예시 많음) :contentReference[oaicite:5]{index=5}
     *
     * 우리는 일관성 있게 POST로 보냄.
     */
    public OAuthTokenResponse exchangeToken(OAuthProvider provider, String code, String state) {
        OAuthProperties.Provider p = providerProps(provider);

        String redirectUri = props.baseUrl()
                + props.redirectPath().replace("{provider}", provider.lower());

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", p.clientId());
        form.add("client_secret", safe(p.clientSecret()));
        form.add("code", code);

        // Kakao는 redirect_uri 필요 :contentReference[oaicite:6]{index=6}
        // Naver도 redirect_uri를 요구할 수 있는 케이스가 있으니 같이 넣어도 무방(없어도 되는 경우가 많음)
        form.add("redirect_uri", redirectUri);

        // Naver는 state를 토큰 요청에 포함 :contentReference[oaicite:7]{index=7}
        if (provider == OAuthProvider.NAVER) {
            form.add("state", state);
        }

        return restClient.post()
                .uri(p.tokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(OAuthTokenResponse.class);
    }

    /**
     * access_token으로 사용자 프로필 조회
     *
     * Kakao: /oauth/token (POST, x-www-form-urlencoded)
     * Naver: /oauth2.0/token (GET/POST 가능, 보통 GET 예시 많음)
     */
    public OAuthUserInfo fetchUserInfo(OAuthProvider provider, String accessToken) {
        OAuthProperties.Provider p = providerProps(provider);

        if (provider == OAuthProvider.KAKAO) {
            KakaoUserInfoResponse r = restClient.get()
                    .uri(p.userinfoUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(KakaoUserInfoResponse.class);

            return normalizeKakao(r);
        }

        NaverUserInfoResponse r = restClient.get()
                .uri(p.userinfoUrl())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(NaverUserInfoResponse.class);

        return normalizeNaver(r);
    }

    // ----------------- normalize -----------------

    private OAuthUserInfo normalizeKakao(KakaoUserInfoResponse r) {
        if (r == null || r.id() == null) {
            throw new CustomException(ErrorCode.OAUTH_USERINFO_FAILED);
        }

        String providerUserId = String.valueOf(r.id());

        String email = null;
        String nickname = null;
        String profileImageUrl = null;

        if (r.kakaoAccount() != null) {
            email = r.kakaoAccount().email();
            if (r.kakaoAccount().profile() != null) {
                nickname = r.kakaoAccount().profile().nickname();
                profileImageUrl = r.kakaoAccount().profile().profileImageUrl();
            }
        }

        // properties에도 닉네임/프로필이 내려올 수 있어 fallback
        if (nickname == null && r.properties() != null) nickname = r.properties().nickname();
        if (profileImageUrl == null && r.properties() != null) profileImageUrl = r.properties().profileImage();

        return new OAuthUserInfo(
                providerUserId,
                email,
                nickname,
                null, // kakao는 보통 name을 안 줌(동의항목/카카오싱크에 따라 다름)
                profileImageUrl
        );
    }

    private OAuthUserInfo normalizeNaver(NaverUserInfoResponse r) {
        if (r == null || r.response() == null || r.response().id() == null) {
            throw new CustomException(ErrorCode.OAUTH_USERINFO_FAILED);
        }

        var u = r.response();

        return new OAuthUserInfo(
                u.id(),
                u.email(),
                u.nickname(),
                u.name(),
                u.profileImage()
        );
    }

    // ----------------- helpers -----------------

    private OAuthProperties.Provider providerProps(OAuthProvider provider) {
        return switch (provider) {
            case KAKAO -> props.kakao();
            case NAVER -> props.naver();
        };
    }

    private String safe(String v) {
        // client_secret이 optional인 provider(카카오 등) 대비
        return v == null ? "" : v;
    }
}