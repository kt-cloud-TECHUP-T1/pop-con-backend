package com.t1.popcon.auction.bid.controller;

import com.t1.popcon.auction.bid.dto.BidRequest;
import com.t1.popcon.auction.bid.dto.BidResponse;
import com.t1.popcon.auction.bid.dto.response.ReservationDetailResponse;
import com.t1.popcon.auction.bid.service.BidService;
import com.t1.popcon.common.auth.domain.AuthUser;
import com.t1.popcon.common.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auctions")
public class BidController {

	private final BidService bidService;

	@PostMapping("/bids")
	public ResponseEntity<ApiResponse<BidResponse>> attemptBid(
		@AuthenticationPrincipal AuthUser authUser,
		@Valid @RequestBody BidRequest request
	) {

		log.info(">>>> [Bid Request] Member ID: {}, Option ID: {}", authUser.id(), request.auctionOptionId());

		BidResponse response = bidService.attemptBid(authUser.id(), request);

		return ResponseEntity.ok(ApiResponse.ok(response));
	}

	@GetMapping("/reservations/{reservationNo}")
	public ResponseEntity<ApiResponse<ReservationDetailResponse>> getReservationDetail(
		@AuthenticationPrincipal AuthUser authUser,
		@PathVariable String reservationNo
	) {
		log.info(">>>> [Reservation Detail Request] Member ID: {}, Reservation No: {}", authUser.id(), reservationNo);

		ReservationDetailResponse response = bidService.getReservationDetail(authUser.id(), reservationNo);

		return ResponseEntity.ok(ApiResponse.ok(response));
	}
}