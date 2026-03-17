package com.t1.popcon.auction.bid.controller;

import com.t1.popcon.auction.bid.dto.BidRequest;
import com.t1.popcon.auction.bid.dto.BidResponse;
import com.t1.popcon.auction.bid.service.BidService;
import com.t1.popcon.common.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auctions")
public class BidController {

	private final BidService bidService;

	@PostMapping("/bids")
	public ResponseEntity<ApiResponse<BidResponse>> attemptBid(
		// TODO: 인증 방식(SecurityContext 등)에 맞춰 memberId 가져와야 함, 헤더로 임시구현
		// 예: @AuthenticationPrincipal UserPrincipal principal
		@RequestHeader(value = "X-Member-Id") Long memberId,
		@Valid @RequestBody BidRequest request
	) {

		log.info(">>>> [Bid Request] Member ID: {}, Option ID: {}", memberId, request.auctionOptionId());

		BidResponse response = bidService.attemptBid(memberId, request);

		return ResponseEntity.ok(ApiResponse.ok(response));
	}
}