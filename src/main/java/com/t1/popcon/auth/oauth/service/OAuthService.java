package com.t1.popcon.auth.oauth.service;

import com.t1.popcon.auth.oauth.config.OAuthProperties;
import com.t1.popcon.auth.oauth.dto.*;
import com.t1.popcon.user.domain.User;
import com.t1.popcon.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

/**
 * OAuth 인가 시작 + 콜백 서비스
 */
@Service
public class OAuthService {

    private final OAuthProperties props;
    private final OAuthStateStore stateStore;
    private final OAuthAuthorizeUrlBuilder urlBuilder;
    private final OAuthClient oAuthClient;

    private final UserRepository userRepository;
    private final RegisterTokenStore registerTokenStore;

    private final SecureRandom random = new SecureRandom();

    public OAuthService(OAuthProperties props,
                        OAuthStateStore stateStore,
                        OAuthAuthorizeUrlBuilder urlBuilder,
                        OAuthClient oAuthClient,
                        UserRepository userRepository,
                        RegisterTokenStore registerTokenStore) {
        this.props = props;
        this.stateStore = stateStore;
        this.urlBuilder = urlBuilder;
        this.oAuthClient = oAuthClient;
        this.userRepository = userRepository;
        this.registerTokenStore = registerTokenStore;
    }

    /**
     * OAuth 로그인 시작: state 생성 → Redis 저장 → authorize url 생성
     */
    public String startAuthorize(OAuthProvider provider) {
        String state = generateRandomUrlSafe(32);

        long ttl = props.stateTtlSeconds();
        if (ttl <= 0) ttl = 300; // 설정 누락 방지용 기본값(5분)

        // ✅ 버그 수정: ttl 변수 사용
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
            throw new IllegalArgumentException("Invalid or expired state");
        }

        // 2) code -> access_token
        OAuthTokenResponse token = oAuthClient.exchangeToken(provider, code, state);
        if (token == null || token.accessToken() == null || token.accessToken().isBlank()) {
            throw new IllegalStateException("Token exchange failed: access_token is empty");
        }

        // 3) access_token -> userinfo
        OAuthUserInfo userInfo = oAuthClient.fetchUserInfo(provider, token.accessToken());

        // 4) 기존회원 조회
        Optional<User> userOpt = findByProvider(provider, userInfo.providerUserId());

        // 4-1) 기존회원: 로그인 완료
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            String accessToken = "stub_access_token_for_user_" + user.getId();
            String refreshToken = "stub_refresh_token_for_user_" + user.getId();

            return SocialLoginResponse.existing(user.getId(), accessToken, refreshToken);
        }

        // 4-2) 신규회원: registerToken 발급 + Redis 저장
        String registerToken = generateRandomUrlSafe(48);

        RegisterPayload payload = new RegisterPayload(
                provider,
                userInfo.providerUserId(),
                userInfo.email(),
                userInfo.nickname(),
                userInfo.name(),
                userInfo.profileImageUrl(),
                null // ciHash는 본인인증 완료 시 merge로 채움
        );

        // TTL은 10분 정도 추천 (너희 정책에 맞게 조정 가능)
        registerTokenStore.save(registerToken, payload, 600);

        // 프론트가 다음 단계로 라우팅할 수 있게 nextStep 제공
        return SocialLoginResponse.newUser(registerToken, "VERIFY_IDENTITY");
    }

    private Optional<User> findByProvider(OAuthProvider provider, String providerUserId) {
        return switch (provider) {
            case KAKAO -> userRepository.findByKakaoUserIdAndDeletedFalse(providerUserId);
            case NAVER -> userRepository.findByNaverUserIdAndDeletedFalse(providerUserId);
        };
    }

    /**
     * URL-safe 랜덤 문자열 생성
     */
    private String generateRandomUrlSafe(int byteLen) {
        byte[] bytes = new byte[byteLen];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}