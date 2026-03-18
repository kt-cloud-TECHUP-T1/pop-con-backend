package com.t1.popcon.auth.oauth.service;

import com.t1.popcon.auth.oauth.client.UserServiceClient;
import com.t1.popcon.auth.oauth.client.dto.UserSocialLookupApiResponse;
import com.t1.popcon.auth.oauth.client.dto.UserSocialLookupResponse;
import com.t1.popcon.auth.oauth.config.OAuthProperties;
import com.t1.popcon.auth.oauth.dto.OAuthTokenResponse;
import com.t1.popcon.auth.oauth.dto.OAuthUserInfo;
import com.t1.popcon.auth.oauth.dto.RegisterPayload;
import com.t1.popcon.auth.oauth.dto.SocialLoginResponse;
import com.t1.popcon.auth.token.domain.RefreshToken;
import com.t1.popcon.auth.token.domain.RefreshTokenRepository;
import com.t1.popcon.common.auth.config.JwtProperties;
import com.t1.popcon.common.auth.domain.TokenType;
import com.t1.popcon.common.auth.provider.TokenProvider;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;

/**
 * OAuth 인가 시작 + 콜백 서비스
 */
@Slf4j
@Service
public class OAuthService {

    private static final String NEXT_STEP_VERIFY_IDENTITY = "VERIFY_IDENTITY";
    private static final long STATE_TTL_DEFAULT_SECONDS = 300L;
    private static final long REGISTER_TTL_SECONDS = 600L;

    private final OAuthProperties props;
    private final OAuthStateStore stateStore;
    private final OAuthAuthorizeUrlBuilder urlBuilder;
    private final OAuthClient oAuthClient;
    private final RegisterTokenStore registerTokenStore;

    private final UserServiceClient userServiceClient;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    private final SecureRandom random = new SecureRandom();

    public OAuthService(
            OAuthProperties props,
            OAuthStateStore stateStore,
            OAuthAuthorizeUrlBuilder urlBuilder,
            OAuthClient oAuthClient,
            RegisterTokenStore registerTokenStore,
            UserServiceClient userServiceClient,
            TokenProvider tokenProvider,
            RefreshTokenRepository refreshTokenRepository,
            JwtProperties jwtProperties
    ) {
        this.props = props;
        this.stateStore = stateStore;
        this.urlBuilder = urlBuilder;
        this.oAuthClient = oAuthClient;
        this.registerTokenStore = registerTokenStore;
        this.userServiceClient = userServiceClient;
        this.tokenProvider = tokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
    }

    /**
     * OAuth 로그인 시작: state 생성 → Redis 저장 → authorize url 생성
     */
    public String startAuthorize(OAuthProvider provider) {
        String state = generateRandomUrlSafe(32);

        long ttl = props.stateTtlSeconds();
        if (ttl <= 0) ttl = STATE_TTL_DEFAULT_SECONDS;

        stateStore.save(state, provider, ttl);
        return urlBuilder.build(provider, state);
    }

    /**
     * OAuth 콜백 처리
     * 1) state 검증
     * 2) code -> token 교환
     * 3) userinfo 조회
     * 4) 기존회원이면 access/refresh 발급
     * 5) 신규회원이면 registerToken 발급 + Redis 저장
     */
    @Transactional
    public SocialLoginResponse handleCallback(OAuthProvider provider, String code, String state) {
        boolean ok = stateStore.consume(state, provider);
        if (!ok) {
            throw new CustomException(ErrorCode.OAUTH_INVALID_STATE);
        }

        OAuthTokenResponse token = oAuthClient.exchangeToken(provider, code, state);
        if (token == null || token.accessToken() == null || token.accessToken().isBlank()) {
            throw new CustomException(ErrorCode.OAUTH_TOKEN_EXCHANGE_FAILED);
        }

        OAuthUserInfo userInfo = oAuthClient.fetchUserInfo(provider, token.accessToken());
        if (userInfo == null || userInfo.providerUserId() == null || userInfo.providerUserId().isBlank()) {
            throw new CustomException(ErrorCode.OAUTH_USERINFO_FAILED);
        }

        UserSocialLookupResponse socialLookup = findByProvider(provider, userInfo.providerUserId());

        if (socialLookup.exists()) {

            String userId = String.valueOf(socialLookup.userId());

            String accessToken = tokenProvider.createToken(
                    userId,
                    jwtProperties.getAccessTokenExpiration(),
                    TokenType.ACCESS
            );

            String refreshToken = tokenProvider.createToken(
                    userId,
                    jwtProperties.getRefreshTokenExpiration(),
                    TokenType.REFRESH
            );

            long refreshTokenExpirationSeconds = jwtProperties.getRefreshTokenExpiration() / 1000;

            refreshTokenRepository.save(
                    RefreshToken.builder()
                            .userId(userId)
                            .token(tokenProvider.hashRefreshToken(refreshToken))
                            .expiration(refreshTokenExpirationSeconds)
                            .build()
            );

            return SocialLoginResponse.existing(socialLookup.userId(), accessToken, refreshToken);
        }

        String registerToken = generateRandomUrlSafe(48);

        RegisterPayload payload = new RegisterPayload(
                provider,
                userInfo.providerUserId(),
                userInfo.email(),
                userInfo.nickname(),
                userInfo.name(),
                userInfo.profileImageUrl(),
                null
        );

        registerTokenStore.save(registerToken, payload, REGISTER_TTL_SECONDS);
        return SocialLoginResponse.newUser(registerToken, NEXT_STEP_VERIFY_IDENTITY);
    }

    private UserSocialLookupResponse findByProvider(OAuthProvider provider, String providerUserId) {
        try {
            UserSocialLookupApiResponse response =
                    userServiceClient.findBySocial(provider.name(), providerUserId);

            if (response.data() == null) {
                return new UserSocialLookupResponse(false, null);
            }
            return response.data();
        } catch (Exception e) {
            throw new CustomException(ErrorCode.ERROR_SYSTEM, "기존 회원 조회에 실패했습니다.");
        }
    }

    private String generateRandomUrlSafe(int byteLen) {
        byte[] bytes = new byte[byteLen];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}