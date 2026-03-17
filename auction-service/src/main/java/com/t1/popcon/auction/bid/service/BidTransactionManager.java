package com.t1.popcon.auction.bid.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.t1.popcon.auction.bid.domain.Bid;
import com.t1.popcon.auction.bid.repository.BidRepository;
import com.t1.popcon.auction.domain.AuctionOption;
import com.t1.popcon.auction.repository.AuctionOptionRepository;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BidTransactionManager {

	private final BidRepository bidRepository;
	private final AuctionOptionRepository auctionOptionRepository;

	// [Step 1] 낙찰 시도 준비: PENDING 상태의 Bid 생성
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Bid preparePendingBid(Long userId, AuctionOption option, Integer price, String merchantUid) {
		return bidRepository.save(Bid.builder()
			.userId(userId)
			.auctionOption(option)
			.bidPrice(price)
			.merchantUid(merchantUid)
			.build());
	}

	// [Step 3-1] 결제 성공 시: SUCCESS 처리 및 DB 재고 차감
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void completeBidSuccess(Long bidId, Long optionId) {
		Bid bid = bidRepository.findById(bidId)
			.orElseThrow(() -> new CustomException(ErrorCode.ERROR_SYSTEM));

		int updatedCount = auctionOptionRepository.decreaseStockAtomic(optionId);

		if (updatedCount == 0) {
			throw new CustomException(ErrorCode.AUCTION_OPTION_SOLD_OUT);
		}

		bid.completePayment(LocalDateTime.now());
	}

	// [Step 3-2] 결제 실패 시: FAILED 처리
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void completeBidFailure(Long bidId) {
		bidRepository.findById(bidId).ifPresent(Bid::failBid);
	}
}