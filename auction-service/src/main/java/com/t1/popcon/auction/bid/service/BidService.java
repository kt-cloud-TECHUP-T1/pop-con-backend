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
import com.t1.popcon.common.infrastructure.portone.PortoneClient;

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
	private final PortoneClient portoneClient;
	private final BidTransactionManager txManager;

	public BidResponse attemptBid(Long userId, BidRequest request) {
		// 1. 조회 및 검증
		AuctionOption option = auctionOptionRepository.findByIdWithAuction(request.auctionOptionId())
			.orElseThrow(() -> new CustomException(ErrorCode.AUCTION_OPTION_NOT_FOUND));

		LocalDateTime now = LocalDateTime.now();
		Auction auction = option.getAuction();
		validateAuctionOpen(auction, now);

		// [가격 검증 로직]
		Integer currentServerPrice = auctionPriceService.calculateCurrentPrice(auction, now);
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
		boolean isPaymentApproved = false;

		try {
			// [Step 1] PENDING 기록 생성
			bid = txManager.preparePendingBid(userId, option, currentServerPrice, merchantUid);

			// [Step 2] 외부 결제 실행
			String billingKey = "DUMMY_KEY";
			portoneClient.executePayment(billingKey, bid.getMerchantUid(), bid.getBidPrice(), "입장권 낙찰");
			isPaymentApproved = true;

			// [Step 3] 최종 확정 (별도 트랜잭션)
			txManager.completeBidSuccess(bid.getId(), option.getId());

			return new BidResponse(bid.getId(), BidStatus.SUCCESS, "낙찰이 완료되었습니다.");

		} catch (Exception e) {
			log.error(">>>> 낙찰 처리 실패 UserId: {}, OptionId: {}", userId, option.getId(), e);

			// [보상 로직] 결제 성공 후 DB 실패 시 결제 취소 호출
			if (isPaymentApproved) {
				try {
					portoneClient.cancelPayment(merchantUid, "시스템 오류로 인한 자동 낙찰 취소");
				} catch (Exception cancelEx) {
					log.error("!!!! [경고] 결제 취소 API 호출 실패: {}", merchantUid, cancelEx);
				}
			}

			bidRedisRepository.incrementStock(option.getId()); // Redis 복구

			if (bid != null) {
				txManager.completeBidFailure(bid.getId()); // DB 상태 FAILED 변경
			}

			throw new CustomException(ErrorCode.PAYMENT_EXECUTION_FAILED, e);
		}
	}

	// 누락되었던 헬퍼 메서드 추가
	private void validateAuctionOpen(Auction auction, LocalDateTime now) {
		if (auctionPriceService.calculateStatus(auction, now) != AuctionStatus.OPEN) {
			throw new CustomException(ErrorCode.AUCTION_NOT_OPEN);
		}
	}
}