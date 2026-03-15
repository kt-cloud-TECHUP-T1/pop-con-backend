package com.t1.popcon.user.billing.controller;

import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.user.billing.dto.BillingKeyInfoResponse;
import com.t1.popcon.user.billing.dto.BillingKeyRegisterRequest;
import com.t1.popcon.user.billing.service.BillingKeyService;
import com.t1.popcon.user.domain.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/billing")
@RequiredArgsConstructor
public class BillingKeyController {

	private final BillingKeyService billingKeyService;

	@PostMapping("/keys")
	public ApiResponse<BillingKeyInfoResponse> register(
		@AuthenticationPrincipal User user,
		@Valid @RequestBody BillingKeyRegisterRequest request
	) {
		BillingKeyInfoResponse response = billingKeyService.registerBillingKey(user.getId(), request);
		return ApiResponse.ok("결제 수단이 성공적으로 등록되었습니다.", response);
	}

	@GetMapping("/my")
	public ApiResponse<Void> getMyBillingKey(
		@AuthenticationPrincipal Long userId
	) {
		// 조회 로직 구현
		return ApiResponse.ok("조회 기능은 준비 중입니다.");
	}
}