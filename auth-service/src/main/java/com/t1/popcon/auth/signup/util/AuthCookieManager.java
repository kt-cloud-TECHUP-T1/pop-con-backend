package com.t1.popcon.auth.signup.util;

import com.t1.popcon.auth.oauth.config.FrontendProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class AuthCookieManager {

    private final FrontendProperties frontendProps;

    public ResponseCookie buildCookie(String name, String value, Duration ttl) {
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
