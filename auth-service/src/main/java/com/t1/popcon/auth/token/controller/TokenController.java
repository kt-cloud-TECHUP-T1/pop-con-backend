package com.t1.popcon.auth.token.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.t1.popcon.auth.token.dto.TokenRefreshRequest;
import com.t1.popcon.auth.token.dto.TokenRefreshResponse;
import com.t1.popcon.auth.token.service.TokenService;
import com.t1.popcon.common.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth/token")
@RequiredArgsConstructor
public class TokenController {

	private final TokenService tokenService;

	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
		TokenRefreshResponse response = tokenService.reissueToken(request);
		return ResponseEntity.ok(ApiResponse.ok("토큰이 재발급되었습니다.", response));
	}
}