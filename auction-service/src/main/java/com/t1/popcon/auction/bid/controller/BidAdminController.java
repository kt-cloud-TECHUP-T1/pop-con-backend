package com.t1.popcon.auction.bid.controller;

import com.t1.popcon.auction.bid.service.BidAdminService;
import com.t1.popcon.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class BidAdminController {

	private final BidAdminService bidAdminService;

	@PostMapping("/auctions/options/{optionId}/init-redis")
	public ResponseEntity<ApiResponse<Void>> initRedisStock(@PathVariable Long optionId) {
		bidAdminService.initStockToRedis(optionId);
		return ResponseEntity.ok(ApiResponse.ok("Redis 재고 초기화 완료 (Option ID: " + optionId + ")"));
	}
}