package com.t1.popcon.auth.oauth.controller;

import com.t1.popcon.auth.oauth.dto.SocialLoginResponse;
import com.t1.popcon.auth.oauth.service.OAuthProvider;
import com.t1.popcon.auth.oauth.service.OAuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * OAuth 인증 컨트롤러
 */
@RestController
@RequestMapping("/auth/oauth")
public class OAuthController {

    private final OAuthService oAuthService;

    public OAuthController(OAuthService oAuthService) {
        this.oAuthService = oAuthService;
    }

    /**
     * OAuth 로그인 시작 (302 Redirect)
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
     * - 기존회원: access/refresh + userId 반환
     * - 신규회원: registerToken + nextStep 반환 (이걸로 본인인증/약관 단계로 넘어감)
     */
    @GetMapping("/{provider}/callback")
    public ResponseEntity<SocialLoginResponse> callback(
            @PathVariable String provider,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(required = false, name = "error_description") String errorDescription
    ) {
        if (error != null) {
            return ResponseEntity.badRequest().build();
        }
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (state == null || state.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        OAuthProvider p = OAuthProvider.from(provider);
        SocialLoginResponse res = oAuthService.handleCallback(p, code, state);
        return ResponseEntity.ok(res);
    }
}
