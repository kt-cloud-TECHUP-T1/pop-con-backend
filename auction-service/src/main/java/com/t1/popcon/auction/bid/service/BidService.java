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
import com.t1.popcon.auction.bid.dto.response.AuctionStatisticsResponse;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BidService {

    private static final String PAYMENT_PRODUCT_NAME = "팝업 입장권 결제";
    private static final String DEFAULT_POPUP_TITLE = "팝업 정보 확인 중";
    private static final String DEFAULT_POPUP_ADDRESS = "주소 확인 중";

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
    private final RedissonClient redissonClient;
    private final MeterRegistry registry;
    private final ObservationRegistry observationRegistry;

    @Transactional(readOnly = true)
    public AuctionStatisticsResponse getAuctionStatistics(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID: " + userId);
        }
        return new AuctionStatisticsResponse(
            bidRepository.countByUserId(userId),
            bidRepository.countByUserIdAndStatus(userId, BidStatus.SUCCESS)
        );
    }
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
        Bid bid = bidRepository.findByReservationNoAndUserId(reservationNo, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.BID_NOT_FOUND));

        PriceDetails priceDetails = computePriceDetails(bid, reservationNo);
        return buildReservationDetailResponse(
                bid,
                priceDetails.startPrice(),
                priceDetails.discountAmount(),
                priceDetails.bidPrice());
    }

    public ReservationDetailResponse getBidDetail(Long userId, Long bidId) {
        Bid bid = bidRepository.findByIdAndUserIdAndStatus(bidId, userId, BidStatus.SUCCESS)
                .orElseThrow(() -> new CustomException(ErrorCode.BID_NOT_FOUND));

        PriceDetails priceDetails = computePriceDetails(bid, bid.getReservationNo());
        return buildReservationDetailResponse(
                bid,
                priceDetails.startPrice(),
                priceDetails.discountAmount(),
                priceDetails.bidPrice());
    }

    private PriceDetails computePriceDetails(Bid bid, String reservationNo) {
        Integer startPrice = bid.getStartPrice();
        Integer bidPrice = bid.getBidPrice();

        if (startPrice == null) {
            log.warn("Start price is null for reservationNo={}, bidId={}", reservationNo, bid.getId());
            startPrice = 0;
        }
        if (bidPrice == null) {
            log.warn("Bid price is null for reservationNo={}, bidId={}, startPrice={}",
                    reservationNo, bid.getId(), bid.getStartPrice());
            bidPrice = 0;
        }

        Integer discountAmount = Math.max(0, startPrice - bidPrice);
        return new PriceDetails(startPrice, bidPrice, discountAmount);
    }

    private ReservationDetailResponse buildReservationDetailResponse(
            Bid bid,
            Integer startPrice,
            Integer discountAmount,
            Integer bidPrice) {
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

    private record PriceDetails(
            Integer startPrice,
            Integer bidPrice,
            Integer discountAmount) {
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

        Long auctionId = option.getAuction().getId();
        RLock lock = redissonClient.getLock("lock:auction:bid:" + userId + ":" + auctionId);

        try {
            if (!lock.tryLock(10, 20, TimeUnit.SECONDS)) {
                throw new CustomException(ErrorCode.ERROR_SYSTEM, "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
            }

            // 1인 1회 제한: 이미 해당 경매에서 낙찰(SUCCESS)된 내역이 있는지 확인
            if (bidRepository.existsByUserIdAndAuctionIdAndStatus(userId, auctionId, BidStatus.SUCCESS)) {
                throw new CustomException(ErrorCode.AUCTION_ALREADY_PARTICIPATED);
            }

            LocalDateTime now = LocalDateTime.now();
            validateAuctionOpen(option.getAuction(), now);

            AuctionStockService.PriceAnchor priceAnchor = auctionStockService.getPriceAnchor(auctionId);
            AuctionStatus auctionStatus = auctionPriceService.calculateStatus(
                option.getAuction(),
                now,
                auctionStockService.hasAvailableStock(auctionId)
            );

            Integer currentServerPrice = auctionPriceService.calculateCurrentPrice(
                option.getAuction(),
                auctionStatus,
                now,
                priceAnchor.soldOutPrice(),
                priceAnchor.restockAnchorAt()
            );
            
            if (!request.bidPrice().equals(currentServerPrice)) {
                Observation.createNotStarted("popcon_bid", observationRegistry)
                        .lowCardinalityKeyValue("outcome", "price_mismatch")
                        .highCardinalityKeyValue("auction_id", String.valueOf(auctionId))
                        .observe(() -> {
                            registry.counter("popcon_bid_total", "outcome", "price_mismatch").increment();
                        });
                throw new CustomException(ErrorCode.AUCTION_PRICE_MISMATCH);
            }

            Long remainingStock = bidRedisRepository.decrementStock(option.getId());
            if (remainingStock < 0) {
                Observation.createNotStarted("popcon_bid", observationRegistry)
                        .lowCardinalityKeyValue("outcome", "sold_out")
                        .highCardinalityKeyValue("auction_id", String.valueOf(auctionId))
                        .observe(() -> {
                            registry.counter("popcon_bid_total", "outcome", "sold_out").increment();
                        });
                throw new CustomException(ErrorCode.AUCTION_OPTION_SOLD_OUT);
            }
            if (remainingStock == 0 && !auctionStockService.hasAvailableStock(auctionId)) {
                auctionStockService.recordSoldOutIfAbsent(auctionId, currentServerPrice);
            }

            String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String merchantUid = "order_no_" + timestamp + "_" + UUID.randomUUID().toString().substring(0, 8);

            Bid bid = null;
            boolean paymentAttempted = false;

            try {
                try {
                    bid = txManager.preparePendingBid(userId, option, currentServerPrice, merchantUid);
                } catch (DataIntegrityViolationException e) {
                    throw new CustomException(ErrorCode.AUCTION_ALREADY_PARTICIPATED);
                }

                ApiResponse<BillingKeyInternalResponse> billingKeyResponse = userBillingClient.getDefaultBillingKey(userId);
                if (billingKeyResponse == null || billingKeyResponse.getData() == null) {
                    Observation.createNotStarted("popcon_bid", observationRegistry)
                            .lowCardinalityKeyValue("outcome", "no_billing_key")
                            .highCardinalityKeyValue("auction_id", String.valueOf(auctionId))
                            .observe(() -> {
                                registry.counter("popcon_bid_total", "outcome", "no_billing_key").increment();
                            });
                    throw new CustomException(ErrorCode.BILLING_KEY_NOT_FOUND);
                }
                String billingKey = billingKeyResponse.getData().customerUid();

                paymentAttempted = true;
                Timer paymentTimer = Timer.builder("popcon_payment_latency")
                        .tag("auction_id", String.valueOf(auctionId))
                        .register(registry);
                Timer.Sample paymentSample = Timer.start(registry);
                PortOnePaymentResponse paymentResponse;
                try {
                    paymentResponse = portOneClient.executePayment(
                        billingKey,
                        bid.getMerchantUid(),
                        bid.getBidPrice(),
                        PAYMENT_PRODUCT_NAME
                    );
                } finally {
                    paymentSample.stop(paymentTimer);
                }

                if (!paymentResponse.isPaid()) {
                    log.error("Payment execution failed because paidAt is missing. merchantUid={}", bid.getMerchantUid());
                    throw new CustomException(ErrorCode.PAYMENT_EXECUTION_FAILED, "Payment was not completed.");
                }

                String pgTxId = paymentResponse.getPgTxId();
                if (pgTxId == null || pgTxId.isBlank()) {
                    log.error("Payment response is missing pgTxId. merchantUid={}", bid.getMerchantUid());
                    throw new CustomException(ErrorCode.PAYMENT_EXECUTION_FAILED, "Payment transaction id is missing.");
                }

                LocalDateTime paidAt = parsePaidAt(paymentResponse.payment().paidAt());

                String popupTitle = DEFAULT_POPUP_TITLE;
                String popupAddress = DEFAULT_POPUP_ADDRESS;
                String thumbnailUrl = null;

                try {
                    ApiResponse<PopupInternalResponse> popupResponse =
                        popupServiceClient.getPopupDetail(option.getAuction().getPopupId());
                    if (popupResponse != null && popupResponse.getData() != null) {
                        PopupInternalResponse popupInfo = popupResponse.getData();
                        if (popupInfo.title() != null) {
                            popupTitle = popupInfo.title();
                        }
                        if (popupInfo.location() != null) {
                            popupAddress = popupInfo.location();
                        }
                        if (popupInfo.vThumbnailUrl() != null) {
                            thumbnailUrl = popupInfo.vThumbnailUrl();
                        }
                    } else {
                        log.warn("Popup detail response is empty. popupId={}", option.getAuction().getPopupId());
                    }
                } catch (Exception e) {
                    log.error("Popup detail lookup failed. popupId={}, error={}",
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
                        log.warn("Reservation number collision detected. retryCount={}, merchantUid={}",
                            retryCount, bid.getMerchantUid());
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
                
                Observation.createNotStarted("popcon_bid", observationRegistry)
                        .lowCardinalityKeyValue("outcome", "success")
                        .highCardinalityKeyValue("auction_id", String.valueOf(auctionId))
                        .observe(() -> {
                            registry.counter("popcon_bid_total", "outcome", "success").increment();
                        });
                        
                return new BidResponse(bid.getId(), BidStatus.SUCCESS, "Bid completed successfully.", reservationNo);

            } catch (Exception e) {
                log.error("Bid processing failed. userId={}, optionId={}, error={}", userId, option.getId(), e.getMessage());

                if (paymentAttempted) {
                    Observation.createNotStarted("popcon_bid", observationRegistry)
                            .lowCardinalityKeyValue("outcome", "payment_failed")
                            .highCardinalityKeyValue("auction_id", String.valueOf(auctionId))
                            .observe(() -> {
                                registry.counter("popcon_bid_total", "outcome", "payment_failed").increment();
                            });
                }

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
                        log.info("Compensation completed. Payment cancel succeeded. merchantUid={}", merchantUid);
                    } else if (cancelResponse.isRequested()) {
                        log.warn("Compensation pending. Payment cancel request accepted. merchantUid={}", merchantUid);
                    } else {
                        log.error("Compensation failed. Payment cancel failed. merchantUid={}, reason={}",
                            merchantUid, cancelResponse.reason());
                    }
                } catch (Exception cancelEx) {
                    log.error("Critical error while calling payment cancel API. merchantUid={}", merchantUid, cancelEx);
                }

                if (cancelConfirmed && bid != null) {
                    txManager.completeBidFailure(bid.getId());
                }

                if (!cancelConfirmed) {
                    throw new CustomException(
                        ErrorCode.PAYMENT_EXECUTION_FAILED,
                        "Payment cancellation is not confirmed, so recovery is deferred."
                    );
                }

                if (e instanceof CustomException customException) {
                    throw customException;
                }
                throw new CustomException(ErrorCode.PAYMENT_EXECUTION_FAILED, e);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException(ErrorCode.ERROR_SYSTEM, "인증 처리 중 오류가 발생했습니다.");
        } finally {
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void validateAuctionOpen(Auction auction, LocalDateTime now) {
        AuctionStatus auctionStatus = auctionPriceService.calculateStatus(
                auction,
                now,
                auctionStockService.hasAvailableStock(auction.getId()));

        if (auctionStatus != AuctionStatus.OPEN) {
            throw new CustomException(ErrorCode.AUCTION_NOT_OPEN);
        }
    }

    private LocalDateTime parsePaidAt(String paidAtStr) {
        if (paidAtStr == null || paidAtStr.isBlank()) {
            throw new CustomException(ErrorCode.PAYMENT_EXECUTION_FAILED, "Payment completion timestamp is missing.");
        }
        try {
            return OffsetDateTime.parse(paidAtStr)
                    .atZoneSameInstant(ZoneId.of("Asia/Seoul"))
                    .toLocalDateTime();
        } catch (Exception e) {
            log.warn("Failed to parse paidAt value: {}", paidAtStr, e);
            throw new CustomException(ErrorCode.PAYMENT_EXECUTION_FAILED,
                    "Failed to parse payment completion timestamp.");
        }
    }

    private void issueAuctionWinTicket(Bid bid, AuctionOption option, String reservationNo) {
        try {
            ApiResponse<TicketIssueResponse> response = ticketServiceClient.issueTicket(
                    buildTicketIssueRequest(bid, option, reservationNo));
            if (response == null || response.getData() == null) {
                throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
            }
        } catch (Exception e) {
            log.error("Ticket service integration failed. bidId={}, optionId={}, error={}",
                    bid.getId(), option.getId(), e.getMessage());
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
                option.getEntryTime());
    }
}
