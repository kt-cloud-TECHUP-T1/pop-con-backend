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

		Integer currentServerPrice = auctionPriceService.calculateCurrentPrice(auction, now);
		if (!request.bidPrice().equals(currentServerPrice)) {
			throw new CustomException(ErrorCode.AUCTION_PRICE_MISMATCH);
		}

		// 2. Redis 재고 선점 및 보상 범위 시작
		Long remainingStock = bidRedisRepository.decrementStock(option.getId());
		if (remainingStock < 0) {
			throw new CustomException(ErrorCode.AUCTION_OPTION_SOLD_OUT);
		}

		String merchantUid = UUID.randomUUID().toString();
		Bid bid = null;

		try {
			// [Step 1] DB에 PENDING 기록 생성
			bid = txManager.preparePendingBid(userId, option, currentServerPrice, merchantUid);

			// [Step 2] 외부 결제 실행 (트랜잭션 외부 - DB 커넥션 반납 상태)
			String billingKey = "DUMMY_KEY"; // Placeholder
			portoneClient.executePayment(billingKey, bid.getMerchantUid(), bid.getBidPrice(), "입장권 낙찰");

			// [Step 3-1] 최종 확정 (성공 트랜잭션)
			txManager.completeBidSuccess(bid.getId(), option.getId());

			return new BidResponse(bid.getId(), BidStatus.SUCCESS, "낙찰이 완료되었습니다.");

		} catch (Exception e) {
			// [Step 3-2] 보상 로직: 모든 실패 시나리오에서 Redis 재고 복구 및 실패 기록
			log.error(">>>> 낙찰 처리 실패 UserId: {}, OptionId: {}", userId, option.getId(), e);

			bidRedisRepository.incrementStock(option.getId()); // Redis 복구

			if (bid != null) {
				txManager.completeBidFailure(bid.getId()); // 실패 기록 저장
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