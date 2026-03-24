package com.t1.popcon.auth.signup.controller;

import com.t1.popcon.auth.signup.dto.SignUpRequest;
import com.t1.popcon.auth.signup.dto.SignUpResponse;
import com.t1.popcon.auth.signup.service.SignUpService;
import com.t1.popcon.auth.signup.util.AuthCookieManager;
import com.t1.popcon.common.auth.config.JwtProperties;
import com.t1.popcon.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class SignUpController {

	private final SignUpService signUpService;
	private final AuthCookieManager cookieManager;
	private final JwtProperties jwtProperties;

	@PostMapping("/signup")
	public ResponseEntity<ApiResponse<SignUpResponse.Signup>> signup(
		@CookieValue(name = "register_token") String registerToken,
		@Valid @RequestBody SignUpRequest.Signup request
	) {
		// 1. 회원가입 처리
		SignUpService.SignupResult result = signUpService.signup(registerToken, request);

		// 2. Refresh Token 쿠키 생성
		ResponseCookie refreshCookie = cookieManager.buildCookie(
			"refresh_token",
			result.refreshToken(),
			Duration.ofMillis(jwtProperties.getRefreshTokenExpiration())
		);

		// 3. 응답 (Set-Cookie 헤더 포함)
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
			.body(ApiResponse.ok("약관 동의 및 회원가입이 완료되었습니다.", result.toResponse()));
	}
}
