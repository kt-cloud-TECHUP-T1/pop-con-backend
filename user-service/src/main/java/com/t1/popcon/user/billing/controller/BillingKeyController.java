package com.t1.popcon.user.billing.controller;

import com.t1.popcon.common.auth.domain.AuthUser;
import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.user.billing.dto.BillingKeyInfoResponse;
import com.t1.popcon.user.billing.dto.BillingKeyRegisterRequest;
import com.t1.popcon.user.billing.service.BillingKeyService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/billing")
@RequiredArgsConstructor
public class BillingKeyController {

	private final BillingKeyService billingKeyService;

	@PostMapping("/keys")
	public ApiResponse<BillingKeyInfoResponse> register(
		@AuthenticationPrincipal AuthUser authUser,
		@Valid @RequestBody BillingKeyRegisterRequest request
	) {
		BillingKeyInfoResponse response = billingKeyService.registerBillingKey(authUser.id(), request);
		return ApiResponse.ok("결제 수단이 성공적으로 등록되었습니다.", response);
	}

	@GetMapping("/my")
	public ApiResponse<List<BillingKeyInfoResponse>> getMyBillingKey(
		@AuthenticationPrincipal AuthUser authUser
	) {
		List<BillingKeyInfoResponse> response = billingKeyService.getMyBillingKeys(authUser.id());
		return ApiResponse.ok("결제 수단 목록을 성공적으로 조회했습니다.", response);
	}

	@PatchMapping("/keys/{id}/default")
	public ApiResponse<Void> changeDefault(
		@AuthenticationPrincipal AuthUser authUser,
		@PathVariable Long id
	) {
		billingKeyService.changeDefaultBillingKey(authUser.id(), id);
		return ApiResponse.ok("대표 결제 수단이 변경되었습니다.");
	}

	@DeleteMapping("/keys/{id}")
	public ApiResponse<Void> delete(
		@AuthenticationPrincipal AuthUser authUser,
		@PathVariable Long id
	) {
		billingKeyService.deleteBillingKey(authUser.id(), id);
		return ApiResponse.ok("결제 수단이 삭제되었습니다.");
	}
}