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
     */
    public OAuthTokenResponse exchangeToken(OAuthProvider provider, String code, String state) {
        try {
            OAuthProperties.Provider p = providerProps(provider);

            String redirectUri = props.baseUrl()
                    + props.redirectPath().replace("{provider}", provider.lower());

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "authorization_code");
            form.add("client_id", p.clientId());
            form.add("code", code);
            form.add("redirect_uri", redirectUri);

            if (p.clientSecret() != null && !p.clientSecret().isBlank()) {
                form.add("client_secret", p.clientSecret());
            }

            if (provider == OAuthProvider.NAVER) {
                form.add("state", state);
            }

            OAuthTokenResponse response = restClient.post()
                    .uri(p.tokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(OAuthTokenResponse.class);

            if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                throw new CustomException(ErrorCode.OAUTH_TOKEN_EXCHANGE_FAILED);
            }

            return response;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.OAUTH_TOKEN_EXCHANGE_FAILED);
        }
    }

    /**
     * access_token으로 사용자 프로필 조회
     */
    public OAuthUserInfo fetchUserInfo(OAuthProvider provider, String accessToken) {
        try {
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
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.OAUTH_USERINFO_FAILED);
        }
    }

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

        if (nickname == null && r.properties() != null) {
            nickname = r.properties().nickname();
        }
        if (profileImageUrl == null && r.properties() != null) {
            profileImageUrl = r.properties().profileImage();
        }

        return new OAuthUserInfo(
                providerUserId,
                email,
                nickname,
                null,
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

    private OAuthProperties.Provider providerProps(OAuthProvider provider) {
        return switch (provider) {
            case KAKAO -> props.kakao();
            case NAVER -> props.naver();
        };
    }
}