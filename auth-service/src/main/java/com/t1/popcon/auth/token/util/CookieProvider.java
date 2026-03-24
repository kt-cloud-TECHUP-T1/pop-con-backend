package com.t1.popcon.auth.token.util;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.t1.popcon.auth.config.FrontendProperties;
import com.t1.popcon.common.auth.config.JwtProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CookieProvider {

	public static final String REFRESH_TOKEN_COOKIE = "refresh_token";
	private final FrontendProperties frontendProps;
	private final JwtProperties jwtProperties;

	public ResponseCookie createRefreshTokenCookie(String refreshToken) {
		return ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
			.httpOnly(true)
			.secure(frontendProps.isCookieSecure())
			.path("/")
			.maxAge(Duration.ofMillis(jwtProperties.getRefreshTokenExpiration()))
			.sameSite(frontendProps.resolvedSameSite())
			.domain(frontendProps.cookieDomain())
			.build();
	}

	public ResponseCookie removeRefreshTokenCookie() {
		return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
			.httpOnly(true)
			.secure(frontendProps.isCookieSecure())
			.path("/")
			.maxAge(0)
			.sameSite(frontendProps.resolvedSameSite())
			.domain(frontendProps.cookieDomain())
			.build();
	}
}
