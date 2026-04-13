package com.t1.popcon.auction.bid.service;

import com.t1.popcon.auction.bid.client.PopupServiceClient;
import com.t1.popcon.auction.bid.client.TicketServiceClient;
import com.t1.popcon.auction.bid.client.UserBillingClient;
import com.t1.popcon.auction.bid.client.dto.BillingKeyInternalResponse;
import com.t1.popcon.auction.bid.client.dto.PopupInternalResponse;
import com.t1.popcon.auction.bid.client.dto.TicketIssueRequest;
import com.t1.popcon.auction.bid.client.dto.TicketIssueResponse;
import com.t1.popcon.auction.bid.domain.Bid;
import com.t1.popcon.auction.bid.domain.BidStatus;
import com.t1.popcon.auction.bid.dto.BidRequest;
import com.t1.popcon.auction.bid.dto.BidResponse;
import com.t1.popcon.auction.bid.dto.response.BidHistoryResponse;
import com.t1.popcon.auction.bid.dto.response.ReservationDetailResponse;
import com.t1.popcon.auction.bid.infrastructure.BidRedisRepository;
import com.t1.popcon.auction.bid.repository.BidRepository;
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
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BidService {

    private final BidRedisRepository bidRedisRepository;
    private final BidRepository bidRepository;
    private final AuctionOptionRepository auctionOptionRepository;
    private final AuctionPriceService auctionPriceService;
    private final AuctionStockService auctionStockService;
    private final PortOneClient portOneClient;
    private final UserBillingClient userBillingClient;
    private final PopupServiceClient popupServiceClient;
    private final BidTransactionManager txManager;
    private final TicketServiceClient ticketServiceClient;
    private final ReservationNoGenerator reservationNoGenerator;

    public Long getAuctionIdByOptionId(Long optionId) {
        return auctionOptionRepository.findByIdWithAuction(optionId)
            .map(option -> option.getAuction().getId())
            .orElseThrow(() -> new CustomException(ErrorCode.AUCTION_OPTION_NOT_FOUND));
    }

    public Long getAuctionIdByReservationNo(String reservationNo) {
        return bidRepository.findByReservationNo(reservationNo)
            .map(Bid::getAuctionId)
            .orElseThrow(() -> new CustomException(ErrorCode.BID_NOT_FOUND));
    }

    public List<BidHistoryResponse> getBidHistory(Long userId) {
        List<Bid> bids = bidRepository.findAllByUserIdAndStatusOrderByCreatedAtDesc(userId, BidStatus.SUCCESS);

        return bids.stream()
            .map(this::convertToHistoryResponse)
            .toList();
    }

    public ReservationDetailResponse getReservationDetail(Long userId, String reservationNo) {
        Bid bid = bidRepository.findByReservationNo(reservationNo)
            .orElseThrow(() -> new CustomException(ErrorCode.BID_NOT_FOUND));

        if (!bid.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        Integer startPrice = bid.getStartPrice() != null ? bid.getStartPrice() : 0;
        Integer bidPrice = bid.getBidPrice();

        Integer discountAmount = Math.max(0, startPrice - bidPrice);
        if (bid.getStartPrice() == null) {
            log.warn(">>>> [데이터 정합성 경고] reservationNo={} 의 startPrice가 null입니다.", reservationNo);
        }

        return ReservationDetailResponse.builder()
            .reservationNo(bid.getReservationNo())
            .popupTitle(bid.getPopupTitle())
            .popupAddress(bid.getPopupAddress())
            .popupThumbnail(bid.getThumbnailUrl())
            .entryDate(bid.getEntryDate())
            .entryTime(bid.getEntryTime())
            .startPrice(startPrice)
            .discountAmount(discountAmount)
            .finalPrice(bidPrice)
            .paidAt(bid.getPaidAt())
            .build();
    }

    public ReservationDetailResponse getBidDetail(Long userId, Long bidId) {
        Bid bid = bidRepository.findByIdAndUserId(bidId, userId)
            .orElseThrow(() -> new CustomException(ErrorCode.BID_NOT_FOUND));

        if (bid.getStartPrice() == null) {
            log.warn(">>>> [데이터 정합성 경고] bidId={}, userId={}, reservationNo={} 의 startPrice가 null입니다.",
                bidId, userId, bid.getReservationNo());
        }

        Integer startPrice = bid.getStartPrice() != null ? bid.getStartPrice() : 0;
        Integer bidPrice = bid.getBidPrice() != null ? bid.getBidPrice() : 0;
        Integer discountAmount = Math.max(0, startPrice - bidPrice);

        return ReservationDetailResponse.builder()
            .reservationNo(bid.getReservationNo())
            .popupTitle(bid.getPopupTitle())
            .popupAddress(bid.getPopupAddress())
            .popupThumbnail(bid.getThumbnailUrl())
            .entryDate(bid.getEntryDate())
            .entryTime(bid.getEntryTime())
            .startPrice(startPrice)
            .discountAmount(discountAmount)
            .finalPrice(bidPrice)
            .paidAt(bid.getPaidAt())
            .build();
    }

    private BidHistoryResponse convertToHistoryResponse(Bid bid) {
        return BidHistoryResponse.builder()
            .id(bid.getId())
            .thumbnailUrl(bid.getThumbnailUrl())
            .popupTitle(bid.getPopupTitle())
            .bidPrice(bid.getBidPrice())
            .paidAt(bid.getPaidAt())
            .displayStatus(bid.getStatus().getDescription())
            .build();
    }

    public BidResponse attemptBid(Long userId, BidRequest request) {
        AuctionOption option = auctionOptionRepository.findByIdWithAuction(request.auctionOptionId())
            .orElseThrow(() -> new CustomException(ErrorCode.AUCTION_OPTION_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        validateAuctionOpen(option.getAuction(), now);

        AuctionStockService.PriceAnchor priceAnchor = auctionStockService.getPriceAnchor(option.getAuction().getId());
        AuctionStatus auctionStatus = auctionPriceService.calculateStatus(
            option.getAuction(),
            now,
            auctionStockService.hasAvailableStock(option.getAuction().getId())
        );

        Integer currentServerPrice = auctionPriceService.calculateCurrentPrice(
            option.getAuction(),
            auctionStatus,
            now,
            priceAnchor.soldOutPrice(),
            priceAnchor.restockAnchorAt()
        );
        if (!request.bidPrice().equals(currentServerPrice)) {
            throw new CustomException(ErrorCode.AUCTION_PRICE_MISMATCH);
        }

        Long remainingStock = bidRedisRepository.decrementStock(option.getId());
        if (remainingStock < 0) {
            throw new CustomException(ErrorCode.AUCTION_OPTION_SOLD_OUT);
        }
        if (remainingStock == 0 && !auctionStockService.hasAvailableStock(option.getAuction().getId())) {
            auctionStockService.recordSoldOutIfAbsent(option.getAuction().getId(), currentServerPrice);
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
                log.error(">>>> [결제 실패] 결제 완료 시각이 없습니다. merchantUid={}", bid.getMerchantUid());
                throw new CustomException(ErrorCode.PAYMENT_EXECUTION_FAILED, "결제가 완료되지 않았습니다.");
            }

            String pgTxId = paymentResponse.getPgTxId();
            if (pgTxId == null || pgTxId.isBlank()) {
                log.error(">>>> [결제 응답 오류] pgTxId 누락 merchantUid={}", bid.getMerchantUid());
                throw new CustomException(ErrorCode.PAYMENT_EXECUTION_FAILED, "결제 트랜잭션 ID가 누락되었습니다.");
            }

            LocalDateTime paidAt = parsePaidAt(paymentResponse.payment().paidAt());

            String popupTitle = "정보 확인 중";
            String popupAddress = "주소 확인 중";
            String thumbnailUrl = null;

            try {
                ApiResponse<PopupInternalResponse> popupResponse = popupServiceClient.getPopupDetail(option.getAuction().getPopupId());
                if (popupResponse != null && popupResponse.getData() != null) {
                    PopupInternalResponse popupInfo = popupResponse.getData();
                    popupTitle = popupInfo.title();
                    popupAddress = popupInfo.location();
                    thumbnailUrl = popupInfo.vThumbnailUrl();
                } else {
                    log.warn(">>>> [팝업 정보 조회 실패] 응답이 비어있음 popupId={}", option.getAuction().getPopupId());
                }
            } catch (Exception e) {
                log.error(">>>> [팝업 정보 조회 오류] 외부 서비스 호출 중 예외 발생 popupId={}, error={}",
                    option.getAuction().getPopupId(), e.getMessage());
            }

            String reservationNo = null;
            int maxRetries = 3;
            int retryCount = 0;

            while (retryCount < maxRetries) {
                try {
                    String currentReservationNo = reservationNoGenerator.generate();
                    reservationNo = txManager.completeBidSuccess(
                        bid.getId(),
                        option.getId(),
                        pgTxId,
                        paidAt,
                        currentReservationNo,
                        popupTitle,
                        popupAddress,
                        thumbnailUrl,
                        option.getEntryDate(),
                        option.getEntryTime(),
                        option.getAuction().getStartPrice()
                    );
                    break;
                } catch (DataIntegrityViolationException e) {
                    retryCount++;
                    log.warn(">>>> [예약번호 충돌] 재시도 중.. retryCount={}, merchantUid={}", retryCount, bid.getMerchantUid());
                    if (retryCount >= maxRetries) {
                        throw e;
                    }
                }
            }

            if (reservationNo == null) {
                reservationNo = bidRepository.findById(bid.getId())
                    .map(Bid::getReservationNo)
                    .orElseThrow(() -> new CustomException(ErrorCode.BID_NOT_FOUND));
            }

            issueAuctionWinTicket(bid, option, reservationNo);
            return new BidResponse(bid.getId(), BidStatus.SUCCESS, "낙찰이 완료되었습니다.", reservationNo);

        } catch (Exception e) {
            log.error(">>>> 낙찰 처리 중 오류 userId={}, optionId={}, error={}", userId, option.getId(), e.getMessage());

            if (!paymentAttempted) {
                bidRedisRepository.incrementAvailableStock(option.getId(), 1L);

                if (bid != null) {
                    txManager.completeBidFailure(bid.getId());
                }

                if (e instanceof CustomException customException) {
                    throw customException;
                }
                throw new CustomException(ErrorCode.PAYMENT_EXECUTION_FAILED, e);
            }

            boolean cancelConfirmed = false;

            try {
                PortOneCancelResponse cancelResponse = portOneClient.cancelPayment(merchantUid, bid.getBidPrice());
                if (cancelResponse.isSucceeded()) {
                    bidRedisRepository.addPendingRestock(option.getId(), 1L);
                    cancelConfirmed = true;
                    log.info(">>>> [보상 완료] 결제 취소 완료 merchantUid={}", merchantUid);
                } else if (cancelResponse.isRequested()) {
                    log.warn(">>>> [보상 보류] 결제 취소 요청만 접수 merchantUid={}", merchantUid);
                } else {
                    log.error("!!!! [보상 실패] 결제 취소 실패 merchantUid={}, reason={}", merchantUid, cancelResponse.reason());
                }
            } catch (Exception cancelEx) {
                log.error("!!!! [긴급] 결제 취소 API 호출 실패 merchantUid={}", merchantUid, cancelEx);
            }

            if (cancelConfirmed && bid != null) {
                txManager.completeBidFailure(bid.getId());
            }

            if (!cancelConfirmed) {
                throw new CustomException(ErrorCode.PAYMENT_EXECUTION_FAILED, "결제 취소가 확정되지 않아 재고 복구를 보류합니다.");
            }

            if (e instanceof CustomException customException) {
                throw customException;
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

    private void issueAuctionWinTicket(Bid bid, AuctionOption option, String reservationNo) {
        try {
            ApiResponse<TicketIssueResponse> response = ticketServiceClient.issueTicket(
                buildTicketIssueRequest(bid, option, reservationNo)
            );
            if (response == null || response.getData() == null) {
                throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
            }
        } catch (Exception e) {
            log.error(">>>> [Ticket-Service 연동 실패] bidId={}, optionId={}, error={}",
                bid.getId(), option.getId(), e.getMessage());
            // TODO: Introduce async compensation/retry flow if ticket issuance must be recovered separately.
            if (e instanceof CustomException customException) {
                throw customException;
            }
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR, e);
        }
    }

    private TicketIssueRequest buildTicketIssueRequest(Bid bid, AuctionOption option, String reservationNo) {
        return new TicketIssueRequest(
            bid.getUserId(),
            option.getAuction().getPopupId(),
            "AUCTION",
            bid.getId(),
            reservationNo,
            option.getEntryDate(),
            option.getEntryTime()
        );
    }
}
