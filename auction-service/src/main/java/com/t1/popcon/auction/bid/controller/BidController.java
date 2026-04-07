package com.t1.popcon.auction.bid.controller;

import com.t1.popcon.auction.bid.dto.BidRequest;
import com.t1.popcon.auction.bid.dto.BidResponse;
import com.t1.popcon.auction.bid.dto.response.ReservationDetailResponse;
import com.t1.popcon.auction.bid.service.BidService;
import com.t1.popcon.common.auth.domain.AuthUser;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.common.queue.QuizPassedTokenInfo;
import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.queue.common.redis.QueueCleanupRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.t1.popcon.common.queue.QuizPassedTokenFilter.QUIZ_PASSED_TOKEN_INFO_ATTRIBUTE;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auctions")
public class BidController {

	private final BidService bidService;
	private final QueueCleanupRepository cleanupRepository;

	@PostMapping("/bids")
	public ResponseEntity<ApiResponse<BidResponse>> attemptBid(
		@AuthenticationPrincipal AuthUser authUser,
		@RequestAttribute(QUIZ_PASSED_TOKEN_INFO_ATTRIBUTE) QuizPassedTokenInfo tokenInfo,
		@Valid @RequestBody BidRequest request
	) {
		Long auctionId = bidService.getAuctionIdByOptionId(request.auctionOptionId());
		validateQuizToken(authUser, tokenInfo, auctionId);
		log.info(">>>> [Bid Request] Member ID: {}, Option ID: {}", authUser.id(), request.auctionOptionId());

		BidResponse response = bidService.attemptBid(authUser.id(), request);

		// 정상 완료 후 대기열 슬롯 반납 (실패해도 비즈니스 로직에 영향을 주지 않도록 예외 처리)
		try {
			cleanupRepository.cleanupUserData(tokenInfo.phaseType(), tokenInfo.phaseId(), authUser.id(), null);
		} catch (Exception e) {
			log.error(">>>> [Cleanup Error] 대기열 슬롯 반납 중 오류 발생 - userId={}, error={}", authUser.id(), e.getMessage());
		}

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

	private void validateQuizToken(AuthUser authUser, QuizPassedTokenInfo tokenInfo, Long auctionId) {
		if (authUser == null || tokenInfo == null) {
			throw new CustomException(ErrorCode.QUIZ_PASSED_TOKEN_MISSING);
		}
		if (!authUser.id().equals(tokenInfo.userId())) {
			throw new CustomException(ErrorCode.QUIZ_PASSED_TOKEN_INVALID, "본인의 퀴즈 결과만 사용할 수 있습니다.");
		}
		if (!"auction".equals(tokenInfo.phaseType())) {
			throw new CustomException(ErrorCode.QUIZ_PASSED_TOKEN_INVALID, "경매 전용 퀴즈 토큰이 아닙니다.");
		}
		if (auctionId != null && !auctionId.equals(tokenInfo.phaseId())) {
			throw new CustomException(ErrorCode.QUIZ_PASSED_TOKEN_INVALID, "해당 경매에 유효한 퀴즈 토큰이 아닙니다.");
		}
	}
}