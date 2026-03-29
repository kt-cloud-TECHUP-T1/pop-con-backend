package com.t1.popcon.auction.bid.service;

import com.t1.popcon.auction.bid.client.UserBillingClient;
import com.t1.popcon.auction.bid.client.dto.BillingKeyInternalResponse;
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
import com.t1.popcon.auction.service.AuctionStockService;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.common.infrastructure.dto.PortOneCancelResponse;
import com.t1.popcon.common.infrastructure.dto.PortOnePaymentResponse;
import com.t1.popcon.common.infrastructure.portone.PortOneClient;
import com.t1.popcon.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BidService {

	private final BidRedisRepository bidRedisRepository;
	private final AuctionOptionRepository auctionOptionRepository;
	private final AuctionPriceService auctionPriceService;
	private final AuctionStockService auctionStockService;
	private final PortOneClient portOneClient;
	private final UserBillingClient userBillingClient;
	private final BidTransactionManager txManager;

	public BidResponse attemptBid(Long userId, BidRequest request) {
		AuctionOption option = auctionOptionRepository.findByIdWithAuction(request.auctionOptionId())
			.orElseThrow(() -> new CustomException(ErrorCode.AUCTION_OPTION_NOT_FOUND));

		LocalDateTime now = LocalDateTime.now();
		validateAuctionOpen(option.getAuction(), now);

		AuctionStatus auctionStatus = auctionPriceService.calculateStatus(
			option.getAuction(),
			now,
			auctionStockService.hasAvailableStock(option.getAuction().getId())
		);

		Integer currentServerPrice = auctionPriceService.calculateCurrentPrice(option.getAuction(), auctionStatus, now);
		if (!request.bidPrice().equals(currentServerPrice)) {
			throw new CustomException(ErrorCode.AUCTION_PRICE_MISMATCH);
		}

		Long remainingStock = bidRedisRepository.decrementStock(option.getId());
		if (remainingStock < 0) {
			throw new CustomException(ErrorCode.AUCTION_OPTION_SOLD_OUT);
		}

		String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
		String merchantUid = "order_no_" + timestamp + "_" + UUID.randomUUID().toString().substring(0, 8);

		Bid bid = null;
		boolean paymentAttempted = false;

		try {
			bid = txManager.preparePendingBid(userId, option, currentServerPrice, merchantUid);

			ApiResponse<BillingKeyInternalResponse> billingKeyResponse = userBillingClient.getDefaultBillingKey(userId);
			if (billingKeyResponse == null || billingKeyResponse.getData() == null) {
				throw new CustomException(ErrorCode.BILLING_KEY_NOT_FOUND);
			}
			String billingKey = billingKeyResponse.getData().customerUid();

			paymentAttempted = true;
			PortOnePaymentResponse paymentResponse = portOneClient.executePayment(
				billingKey,
				bid.getMerchantUid(),
				bid.getBidPrice(),
				"입장권 낙찰"
			);

			if (!paymentResponse.isPaid()) {
				log.error(">>>> [결제 실패] 결제 완료 일시가 없습니다. merchantUid={}", bid.getMerchantUid());
				throw new CustomException(ErrorCode.PAYMENT_EXECUTION_FAILED, "결제가 완료되지 않았습니다.");
			}

			String pgTxId = paymentResponse.getPgTxId();
			if (pgTxId == null || pgTxId.isBlank()) {
				log.error(">>>> [결제 응답 오류] pgTxId 누락 merchantUid={}", bid.getMerchantUid());
				throw new CustomException(ErrorCode.PAYMENT_EXECUTION_FAILED, "결제 트랜잭션 ID가 누락되었습니다.");
			}

			LocalDateTime paidAt = parsePaidAt(paymentResponse.payment().paidAt());
			txManager.completeBidSuccess(bid.getId(), option.getId(), pgTxId, paidAt);

			return new BidResponse(bid.getId(), BidStatus.SUCCESS, "낙찰이 완료되었습니다.");

		} catch (Exception e) {
			log.error(">>>> 낙찰 처리 중 오류 userId={}, optionId={}, error={}", userId, option.getId(), e.getMessage());

			if (paymentAttempted) {
				try {
					PortOneCancelResponse cancelResponse = portOneClient.cancelPayment(merchantUid, bid.getBidPrice());
					if (cancelResponse.isSucceeded()) {
						log.info(">>>> [보상 완료] 결제 취소 완료 merchantUid={}", merchantUid);
					} else if (cancelResponse.isRequested()) {
						log.warn(">>>> [보상 진행중] 결제 취소가 접수되었습니다. merchantUid={}", merchantUid);
					} else {
						log.error("!!!! [보상 실패] 결제 취소 실패 merchantUid={}, reason={}", merchantUid, cancelResponse.reason());
					}
				} catch (Exception cancelEx) {
					log.error("!!!! [긴급] 결제 취소 API 호출 실패 merchantUid={}", merchantUid, cancelEx);
				}
			}

			bidRedisRepository.addPendingRestock(option.getId(), 1L);

			if (bid != null) {
				txManager.completeBidFailure(bid.getId());
			}

			throw new CustomException(ErrorCode.PAYMENT_EXECUTION_FAILED, e);
		}
	}

	private void validateAuctionOpen(Auction auction, LocalDateTime now) {
		AuctionStatus auctionStatus = auctionPriceService.calculateStatus(
			auction,
			now,
			auctionStockService.hasAvailableStock(auction.getId())
		);

		if (auctionStatus != AuctionStatus.OPEN) {
			throw new CustomException(ErrorCode.AUCTION_NOT_OPEN);
		}
	}

	private LocalDateTime parsePaidAt(String paidAtStr) {
		if (paidAtStr == null || paidAtStr.isBlank()) {
			throw new CustomException(ErrorCode.PAYMENT_EXECUTION_FAILED, "결제 완료 시각이 누락되었습니다.");
		}
		try {
			return OffsetDateTime.parse(paidAtStr).toLocalDateTime();
		} catch (Exception e) {
			log.warn(">>>> [paidAt 파싱 실패] {}", paidAtStr, e);
			throw new CustomException(ErrorCode.PAYMENT_EXECUTION_FAILED, "결제 완료 시각 파싱에 실패했습니다.");
		}
	}
}
