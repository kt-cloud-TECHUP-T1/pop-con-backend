package com.t1.popcon.auction.bid.service;

import com.t1.popcon.auction.bid.domain.Bid;
import com.t1.popcon.auction.bid.domain.BidStatus;
import com.t1.popcon.auction.bid.dto.BidRequest;
import com.t1.popcon.auction.bid.dto.BidResponse;
import com.t1.popcon.auction.bid.infrastructure.BidRedisRepository;
import com.t1.popcon.auction.domain.Auction;
import com.t1.popcon.auction.domain.AuctionOption;
import com.t1.popcon.auction.domain.AuctionStatus;
import com.t1.popcon.auction.repository.AuctionOptionRepository;
import com.t1.popcon.auction.service.AuctionPriceService;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.common.infrastructure.portone.PortOneClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BidService {

	private final BidRedisRepository bidRedisRepository;
	private final AuctionOptionRepository auctionOptionRepository;
	private final AuctionPriceService auctionPriceService;
	private final PortOneClient portOneClient;
	private final BidTransactionManager txManager;

	public BidResponse attemptBid(Long userId, BidRequest request) {
		// 1. 조회 및 검증
		AuctionOption option = auctionOptionRepository.findByIdWithAuction(request.auctionOptionId())
			.orElseThrow(() -> new CustomException(ErrorCode.AUCTION_OPTION_NOT_FOUND));

		LocalDateTime now = LocalDateTime.now();
		validateAuctionOpen(option.getAuction(), now);

		Integer currentServerPrice = auctionPriceService.calculateCurrentPrice(option.getAuction(), now);
		if (!request.bidPrice().equals(currentServerPrice)) {
			throw new CustomException(ErrorCode.AUCTION_PRICE_MISMATCH);
		}

		// 2. Redis 재고 선점
		Long remainingStock = bidRedisRepository.decrementStock(option.getId());
		if (remainingStock < 0) {
			throw new CustomException(ErrorCode.AUCTION_OPTION_SOLD_OUT);
		}

		String merchantUid = UUID.randomUUID().toString();
		Bid bid = null;
		boolean paymentAttempted = false;

		try {
			// [Step 1] PENDING 기록 생성
			bid = txManager.preparePendingBid(userId, option, currentServerPrice, merchantUid);

			// [Step 2] 외부 결제 실행
			String billingKey = "DUMMY_KEY";

			// 결제 요청 직전에 시도 플래그를 세팅합니다.
			paymentAttempted = true;
			portOneClient.executePayment(billingKey, bid.getMerchantUid(), bid.getBidPrice(), "입장권 낙찰");

			// [Step 3] 최종 확정 (DB 트랜잭션)
			txManager.completeBidSuccess(bid.getId(), option.getId());

			return new BidResponse(bid.getId(), BidStatus.SUCCESS, "낙찰이 완료되었습니다.");

		} catch (Exception e) {
			log.error(">>>> 낙찰 처리 중 장애 발생: UserId {}, OptionId {}, Error: {}", userId, option.getId(), e.getMessage());

			if (paymentAttempted) {
				try {
					portOneClient.cancelPayment(merchantUid, "시스템 오류로 인한 자동 낙찰 취소");
					log.info(">>>> [보상 완료] 결제 취소 요청 성공: {}", merchantUid);
				} catch (Exception cancelEx) {
					log.error("!!!! [긴급] 결제 취소 API 호출 실패 - 수동 확인 필요: {}", merchantUid, cancelEx);
				}
			}

			// Redis 재고 복구
			bidRedisRepository.incrementStock(option.getId());

			// DB 상태 실패 처리
			if (bid != null) {
				txManager.completeBidFailure(bid.getId());
			}

			throw new CustomException(ErrorCode.PAYMENT_EXECUTION_FAILED, e);
		}
	}

	private void validateAuctionOpen(Auction auction, LocalDateTime now) {
		if (auctionPriceService.calculateStatus(auction, now) != AuctionStatus.OPEN) {
			throw new CustomException(ErrorCode.AUCTION_NOT_OPEN);
		}
	}
}