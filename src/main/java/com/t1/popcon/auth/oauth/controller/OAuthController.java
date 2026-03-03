package com.t1.popcon.auth.oauth.controller;

import com.t1.popcon.auth.oauth.config.FrontendProperties;
import com.t1.popcon.auth.oauth.dto.SocialLoginResponse;
import com.t1.popcon.auth.oauth.service.OAuthProvider;
import com.t1.popcon.auth.oauth.service.OAuthService;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;

/**
 * OAuth 인증 컨트롤러
 */
@RestController
@RequestMapping("/auth/oauth")
public class OAuthController {

    private static final String REGISTER_TOKEN_COOKIE = "register_token";
    private static final Duration REGISTER_TOKEN_TTL = Duration.ofMinutes(10);

    private final OAuthService oAuthService;
    private final FrontendProperties frontendProps;

    public OAuthController(OAuthService oAuthService, FrontendProperties frontendProps) {
        this.oAuthService = oAuthService;
        this.frontendProps = frontendProps;
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
     * OAuth 콜백 (방식 A)
     * - 성공(기존회원) → FE /
     * - 성공(신규회원) → FE /verify (+ register_token 쿠키)
     * - 실패(에러)     → FE /login?error={CODE}
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
            return redirect(frontendErrorUrl("A001"), null);
        }

        // code/state 누락
        if (code == null || code.isBlank() || state == null || state.isBlank()) {
            return redirect(frontendErrorUrl("C001"), null);
        }

        OAuthProvider p = OAuthProvider.from(provider);

        try {
            SocialLoginResponse res = oAuthService.handleCallback(p, code, state);

            if (res.isNewUser()) {
                // ✅ 신규회원: register_token 쿠키 심고 /verify로 이동
                ResponseCookie cookie = buildRegisterTokenCookie(res.registerToken());
                return redirect(frontendProps.verifyUrl(), cookie);
            }

            // ✅ 기존회원: /로 이동 (토큰은 추후 쿠키/세션 API로 붙이기)
            return redirect(frontendProps.homeUrl(), null);

        } catch (CustomException e) {
            // ✅ 공통 예외는 ErrorCode.code를 프론트에 전달
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

    /**
     * registerToken을 HttpOnly 쿠키로 설정
     * - local(http): secure=false, SameSite=Lax
     * - dev/prod(https + FE/BE 도메인 분리): SameSite=None + secure=true + domain 필요
     *
     * 지금은 local 기준값.
     */
    private ResponseCookie buildRegisterTokenCookie(String registerToken) {
        return ResponseCookie.from(REGISTER_TOKEN_COOKIE, registerToken)
                .httpOnly(true)
                .secure(false)     // ✅ local 기준. dev/prod는 true로 분기 필요
                .sameSite("Lax")   // ✅ local 기준. dev/prod는 None 권장
                .path("/")
                .maxAge(REGISTER_TOKEN_TTL)
                .build();
    }
}