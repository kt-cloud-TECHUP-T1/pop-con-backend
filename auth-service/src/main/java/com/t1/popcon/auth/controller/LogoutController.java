package com.t1.popcon.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.t1.popcon.auth.token.service.TokenService;
import com.t1.popcon.auth.token.util.CookieProvider;
import com.t1.popcon.common.auth.domain.AuthUser;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.common.response.ApiResponse;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class LogoutController {

	private final TokenService tokenService;
	private final CookieProvider cookieProvider;

	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(
		@AuthenticationPrincipal AuthUser authUser,
		HttpServletResponse response
	) {
		if (authUser == null || authUser.id() == null) {
			throw new CustomException(ErrorCode.INVALID_TOKEN);
		}

		tokenService.logout(authUser.id());

		ResponseCookie clearCookie = cookieProvider.removeRefreshTokenCookie();
		response.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());

		return ResponseEntity.ok(ApiResponse.ok("로그아웃되었습니다."));
	}
}
