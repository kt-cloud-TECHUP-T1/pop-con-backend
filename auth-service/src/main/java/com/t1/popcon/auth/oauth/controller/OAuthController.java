package com.t1.popcon.auth.oauth.controller;

import com.t1.popcon.auth.config.FrontendProperties;
import com.t1.popcon.auth.oauth.dto.SocialLoginResponse;
import com.t1.popcon.auth.oauth.service.OAuthProvider;
import com.t1.popcon.auth.oauth.service.OAuthService;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.auth.config.JwtProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;

/**
 * OAuth 인증 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/auth/oauth")
public class OAuthController {

    private static final String REGISTER_TOKEN_COOKIE = "register_token";
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private static final Duration REGISTER_TOKEN_TTL = Duration.ofMinutes(10);
    private final Duration refreshTokenTtl;

    private final OAuthService oAuthService;
    private final FrontendProperties frontendProps;

    public OAuthController(OAuthService oAuthService, FrontendProperties frontendProps, JwtProperties jwtProperties) {
        this.oAuthService = oAuthService;
        this.frontendProps = frontendProps;
        this.refreshTokenTtl = Duration.ofMillis(jwtProperties.getRefreshTokenExpiration());
    }

    /**
     * OAuth 로그인 시작 (302 Redirect → Provider authorize URL)
     */
    @GetMapping("/{provider}")
    public ResponseEntity<Void> oauthRedirect(@PathVariable String provider) {
        OAuthProvider p = OAuthProvider.from(provider);
        String authorizeUrl = oAuthService.startAuthorize(p);

        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, authorizeUrl)
                .build();
    }

    /**
     * OAuth 콜백
     * - 기존회원 → refresh_token 쿠키 세팅 후 FE callback 경로 이동
     * - 신규회원 → register_token 쿠키 세팅 후 FE verify 경로 이동
     * - 실패     → FE login?error={CODE}
     */
    @GetMapping("/{provider}/callback")
    public ResponseEntity<Void> callback(
            @PathVariable String provider,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(required = false, name = "error_description") String errorDescription
    ) {
        // Provider 에러(사용자 취소 등)
        if (error != null && !error.isBlank()) {
            return redirect(frontendErrorUrl("OA001"), null);
        }

        // code/state 누락
        if (code == null || code.isBlank() || state == null || state.isBlank()) {
            return redirect(frontendErrorUrl("C001"), null);
        }

        OAuthProvider p = OAuthProvider.from(provider);

        try {
            SocialLoginResponse res = oAuthService.handleCallback(p, code, state);

            if (res.isNewUser()) {
                ResponseCookie registerCookie = buildCookie(
                        REGISTER_TOKEN_COOKIE,
                        res.registerToken(),
                        REGISTER_TOKEN_TTL
                );
                return redirect(frontendProps.verifyUrl(), registerCookie);
            }

            ResponseCookie refreshCookie = buildCookie(
                    REFRESH_TOKEN_COOKIE,
                    res.refreshToken(),
                    refreshTokenTtl
            );

            return redirect(frontendProps.callbackUrl(), refreshCookie);

        } catch (CustomException e) {
            log.error("oauth callback failed provider={}, errorCode={}", provider, e.getErrorCode().getCode(), e);
            return redirect(frontendErrorUrl(e.getErrorCode().getCode()), null);
        }
    }

    private ResponseEntity<Void> redirect(String location, ResponseCookie cookie) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, location);

        if (cookie != null) {
            builder.header(HttpHeaders.SET_COOKIE, cookie.toString());
        }

        return builder.build();
    }

    private String frontendErrorUrl(String code) {
        return UriComponentsBuilder
                .fromHttpUrl(frontendProps.loginUrl())
                .queryParam("error", code)
                .build(true)
                .toUriString();
    }

    private ResponseCookie buildCookie(String name, String value, Duration ttl) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(frontendProps.isCookieSecure())
                .sameSite(frontendProps.resolvedSameSite())
                .path("/")
                .maxAge(ttl);

        if (frontendProps.cookieDomain() != null && !frontendProps.cookieDomain().isBlank()) {
            builder.domain(frontendProps.cookieDomain());
        }

        return builder.build();
    }
}