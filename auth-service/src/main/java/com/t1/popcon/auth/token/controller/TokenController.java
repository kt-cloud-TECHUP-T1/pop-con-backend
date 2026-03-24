package com.t1.popcon.auth.token.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.t1.popcon.auth.token.dto.TokenRefreshResponse;
import com.t1.popcon.auth.token.service.TokenService;
import com.t1.popcon.auth.token.service.TokenService.TokenReissueResult;
import com.t1.popcon.auth.token.util.CookieProvider;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.common.response.ApiResponse;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth/token")
@RequiredArgsConstructor
public class TokenController {

	private final TokenService tokenService;
	private final CookieProvider cookieProvider;

	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshToken(
		@CookieValue(name = CookieProvider.REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
		HttpServletResponse response) {

		if (refreshToken == null || refreshToken.isBlank()) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}

		TokenReissueResult result = tokenService.reissueToken(refreshToken);

		// 새로운 Refresh Token을 쿠키에 설정
		ResponseCookie cookie = cookieProvider.createRefreshTokenCookie(result.refreshToken());
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

		// Access Token은 응답 바디로 반환
		TokenRefreshResponse tokenResponse = TokenRefreshResponse.builder()
			.accessToken(result.accessToken())
			.expiresIn(result.expiresIn())
			.build();

		return ResponseEntity.ok(ApiResponse.ok("토큰이 재발급되었습니다.", tokenResponse));
	}
}
