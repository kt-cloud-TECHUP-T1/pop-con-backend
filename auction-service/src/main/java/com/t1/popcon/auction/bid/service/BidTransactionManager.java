package com.t1.popcon.auction.bid.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.t1.popcon.auction.bid.domain.Bid;
import com.t1.popcon.auction.bid.domain.BidStatus;
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

	// [Step 3-1] 결제 성공 시: SUCCESS 처리 및 스냅샷 기록, DB 재고 차감
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public String completeBidSuccess(Long bidId, Long optionId, String pgTxId, LocalDateTime paidAt,
								   String paymentMethod, String pgProvider, String cardName, String cardNumber,
								   String reservationNo, String popupTitle, String popupAddress, String thumbnailUrl,
								   LocalDate entryDate, LocalTime entryTime, Integer startPrice) {

		// 1. 상태 전이 시도 (PENDING -> SUCCESS) 및 스냅샷/예약번호 업데이트
		int updatedRows = bidRepository.updateStatusWithCAS(
			bidId, BidStatus.PENDING, BidStatus.SUCCESS, paidAt, pgTxId,
			paymentMethod, pgProvider, cardName, cardNumber,
			reservationNo, popupTitle, popupAddress, thumbnailUrl, entryDate, entryTime, startPrice
		);

		// 이미 SUCCESS이거나 다른 상태라면 로직 중단 (멱등성 보장)
		if (updatedRows == 0) {
			return null;
		}

		// 2. 상태 전이에 성공한 경우에만 실제 DB 재고 차감
		int updatedStock = auctionOptionRepository.decreaseStockAtomic(optionId);

		// 만약 Redis는 통과했지만 DB 재고 차감 시점에 재고가 없다면 예외 발생
		if (updatedStock == 0) {
			throw new CustomException(ErrorCode.AUCTION_OPTION_SOLD_OUT);
		}

		return reservationNo;
	}

	// [Step 3-2] 결제 실패 시: FAILED 처리
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void completeBidFailure(Long bidId) {
		bidRepository.findById(bidId).ifPresent(Bid::failBid);
	}
}
