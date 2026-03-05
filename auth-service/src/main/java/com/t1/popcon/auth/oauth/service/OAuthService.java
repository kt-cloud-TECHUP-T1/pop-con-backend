package com.t1.popcon.auth.oauth.service;

import com.t1.popcon.auth.oauth.config.OAuthProperties;
import com.t1.popcon.auth.oauth.dto.*;
//import com.t1.popcon.user.domain.User;
//import com.t1.popcon.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

/**
 * OAuth 인가 시작 + 콜백 서비스
 */
@Service
public class OAuthService {

    private static final String NEXT_STEP_VERIFY_IDENTITY = "VERIFY_IDENTITY";
    private static final long STATE_TTL_DEFAULT_SECONDS = 300L;
    private static final long REGISTER_TTL_SECONDS = 600L;

    private final OAuthProperties props;
    private final OAuthStateStore stateStore;
    private final OAuthAuthorizeUrlBuilder urlBuilder;
    private final OAuthClient oAuthClient;

    //private final UserRepository userRepository;
    private final RegisterTokenStore registerTokenStore;

    private final SecureRandom random = new SecureRandom();

    public OAuthService(OAuthProperties props,
                        OAuthStateStore stateStore,
                        OAuthAuthorizeUrlBuilder urlBuilder,
                        OAuthClient oAuthClient,
                        //UserRepository userRepository,
                        RegisterTokenStore registerTokenStore) {
        this.props = props;
        this.stateStore = stateStore;
        this.urlBuilder = urlBuilder;
        this.oAuthClient = oAuthClient;
        //this.userRepository = userRepository;
        this.registerTokenStore = registerTokenStore;
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
     * OAuth 콜백 처리 (최종 분기)
     * 1) state 검증(1회성 consume)
     * 2) code -> token 교환
     * 3) userinfo 조회
     * 4) 기존회원이면 로그인 응답 / 신규회원이면 registerToken 발급
     */
    public SocialLoginResponse handleCallback(OAuthProvider provider, String code, String state) {
        // 1) state 검증 (CSRF 방지 + 1회성)
        boolean ok = stateStore.consume(state, provider);
        if (!ok) {
            throw new CustomException(ErrorCode.OAUTH_INVALID_STATE);
        }

        // 2) code -> access_token
        OAuthTokenResponse token = oAuthClient.exchangeToken(provider, code, state);
        if (token == null || token.accessToken() == null || token.accessToken().isBlank()) {
            throw new CustomException(ErrorCode.OAUTH_TOKEN_EXCHANGE_FAILED);
        }

        // 3) access_token -> userinfo
        OAuthUserInfo userInfo = oAuthClient.fetchUserInfo(provider, token.accessToken());
        if (userInfo == null || userInfo.providerUserId() == null || userInfo.providerUserId().isBlank()) {
            // userInfo가 비정상이면 이후 로직 진행 불가
            throw new CustomException(ErrorCode.OAUTH_USERINFO_FAILED);
        }

        // 4) 기존회원 조회
        //Optional<User> userOpt = findByProvider(provider, userInfo.providerUserId());

        // 4-1) 기존회원: 로그인 완료
        /*if (userOpt.isPresent()) {
            User user = userOpt.get();

            // TODO: JWT로 교체 예정
            String accessToken = "stub_access_token_for_user_" + user.getId();
            String refreshToken = "stub_refresh_token_for_user_" + user.getId();

            return SocialLoginResponse.existing(user.getId(), accessToken, refreshToken);
        }*/

        // 4-2) 신규회원: registerToken 발급 + Redis 저장
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

    /*private Optional<User> findByProvider(OAuthProvider provider, String providerUserId) {
        return switch (provider) {
            case KAKAO -> userRepository.findByKakaoUserIdAndDeletedFalse(providerUserId);
            case NAVER -> userRepository.findByNaverUserIdAndDeletedFalse(providerUserId);
        };
    }*/

    private String generateRandomUrlSafe(int byteLen) {
        byte[] bytes = new byte[byteLen];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}