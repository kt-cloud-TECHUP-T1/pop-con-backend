package com.t1.popcon.auth.signup.controller;

import com.t1.popcon.auth.signup.dto.SignUpRequest;
import com.t1.popcon.auth.signup.dto.SignUpResponse;
import com.t1.popcon.auth.signup.service.SignUpService;
import com.t1.popcon.common.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class SignUpController {

	private final SignUpService authService;

	@PostMapping("/signup")
	public ResponseEntity<ApiResponse<SignUpResponse.Signup>> signup(
		@Valid @RequestBody SignUpRequest.Signup request
	) {
		SignUpResponse.Signup response = authService.signup(request);

		return ResponseEntity.ok(ApiResponse.ok("약관 동의 및 회원가입이 완료되었습니다.", response));
	}
}