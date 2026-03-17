package com.t1.popcon.auction.bid.service;

import com.t1.popcon.auction.bid.domain.Bid;
import com.t1.popcon.auction.bid.domain.BidStatus;
import com.t1.popcon.auction.bid.dto.BidRequest;
import com.t1.popcon.auction.bid.dto.BidResponse;
import com.t1.popcon.auction.bid.infrastructure.BidRedisRepository;
import com.t1.popcon.auction.bid.repository.BidRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BidService {

	private final BidRedisRepository bidRedisRepository;
	private final BidRepository bidRepository;
	private final AuctionOptionRepository auctionOptionRepository;
	private final AuctionPriceService auctionPriceService;
	private final PortoneClient portoneClient;

	@Transactional
	public BidResponse attemptBid(Long userId, BidRequest request) {
		// [1] 사전 검증: 회차 존재 및 경매 진행 여부 확인
		AuctionOption option = auctionOptionRepository.findByIdWithAuction(request.auctionOptionId())
			.orElseThrow(() -> new CustomException(ErrorCode.AUCTION_OPTION_NOT_FOUND));

		LocalDateTime now = LocalDateTime.now();
		Auction auction = option.getAuction();
		validateAuctionOpen(auction, now);

		// [2] 가격 검증: 사용자가 보낸 가격과 서버 계산 현재가 비교
		Integer currentServerPrice = auctionPriceService.calculateCurrentPrice(auction, now);
		if (!request.bidPrice().equals(currentServerPrice)) {
			throw new CustomException(ErrorCode.AUCTION_PRICE_MISMATCH);
		}

		// [3] Redis : Lua Script로 재고 선점
		Long remainingStock = bidRedisRepository.decrementStock(option.getId());
		if (remainingStock < 0) {
			throw new CustomException(ErrorCode.AUCTION_OPTION_SOLD_OUT);
		}

		// [4] 낙찰 기록 생성 (PENDING)
		String merchantUid = UUID.randomUUID().toString();
		Bid bid = bidRepository.save(Bid.builder()
			.userId(userId)
			.auctionOption(option)
			.bidPrice(currentServerPrice)
			.merchantUid(merchantUid)
			.build());

		try {
			// [5] 실제 결제 실행
			String billingKey = "USER_BILLING_KEY_FROM_USER_SERVICE"; // 추후 연동 필요
			portoneClient.executePayment(billingKey, bid.getMerchantUid(), bid.getBidPrice(), "팝업스토어 입장권 낙찰");

			// [6] 최종 확정 로직 (메서드 내 직접 구현으로 트랜잭션 보장)
			bid.completePayment(LocalDateTime.now());
			option.decreaseStock(1);

			return new BidResponse(bid.getId(), BidStatus.SUCCESS, "낙찰이 완료되었습니다.");

		} catch (Exception e) {
			// [7] 보상 트랜잭션: 결제 실패 시 Redis 재고 복구
			log.error("결제 실패 - userId: {}, optionId: {}", userId, option.getId(), e);
			bid.failBid();
			bidRedisRepository.incrementStock(option.getId());
			throw new CustomException(ErrorCode.PAYMENT_EXECUTION_FAILED, e);
		}
	}

	private void validateAuctionOpen(Auction auction, LocalDateTime now) {
		if (auctionPriceService.calculateStatus(auction, now) != AuctionStatus.OPEN) {
			throw new CustomException(ErrorCode.AUCTION_NOT_OPEN);
		}
	}
}