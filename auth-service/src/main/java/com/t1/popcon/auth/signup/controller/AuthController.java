package com.t1.popcon.auth.signup.controller;

import com.t1.popcon.auth.signup.dto.AuthRequest;
import com.t1.popcon.auth.signup.dto.AuthResponse;
import com.t1.popcon.auth.signup.service.AuthService;
import com.t1.popcon.common.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/signup")
	public ResponseEntity<ApiResponse<AuthResponse.Signup>> signup(
		@Valid @RequestBody AuthRequest.Signup request
	) {
		AuthResponse.Signup response = authService.signup(request);

		return ResponseEntity.ok(ApiResponse.ok("약관 동의 및 회원가입이 완료되었습니다.", response));
	}
}