package com.t1.popcon.auction.scheduler;

import com.t1.popcon.auction.domain.Auction;
import com.t1.popcon.auction.domain.AuctionButtonStatus;
import com.t1.popcon.auction.domain.AuctionStatus;
import com.t1.popcon.auction.dto.response.AuctionPriceStreamResponse;
import com.t1.popcon.auction.repository.AuctionRepository;
import com.t1.popcon.auction.service.AuctionPriceService;
import com.t1.popcon.auction.service.AuctionSseService;
import com.t1.popcon.auction.service.AuctionStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionPriceBroadcastScheduler {

    private final AuctionRepository auctionRepository;
    private final AuctionPriceService auctionPriceService;
    private final AuctionStockService auctionStockService;
    private final AuctionSseService auctionSseService;

    @Transactional
    @Scheduled(fixedRate = 1000)
    public void broadcastAuctionPrices() {
        LocalDateTime now = LocalDateTime.now();

        List<Auction> auctions = auctionRepository.findAllByStatusIn(
                List.of(AuctionStatus.SCHEDULED, AuctionStatus.OPEN, AuctionStatus.SOLD_OUT)
        );

        for (Auction auction : auctions) {
            if (auctionPriceService.isPriceDropBoundary(auction, now)) {
                auctionStockService.releasePendingRestocks(auction.getId());
            }

            AuctionStatus calculatedStatus = auctionPriceService.calculateStatus(
                    auction,
                    now,
                    auctionStockService.hasAvailableStock(auction.getId())
            );

            if (auction.getStatus() != calculatedStatus) {
                auction.updateStatus(calculatedStatus);
            }

            if (!auctionSseService.hasSubscribers(auction.getId())) {
                continue;
            }

            Long remainingUntilOpenSeconds = auctionPriceService.calculateRemainingUntilOpenSeconds(auction, now);
            Long remainingUntilCloseSeconds = auctionPriceService.calculateRemainingUntilCloseSeconds(auction, calculatedStatus, now);

            Integer currentPrice = auctionPriceService.calculateCurrentPrice(auction, calculatedStatus, now);
            Integer nextPrice = auctionPriceService.calculateNextPrice(auction, currentPrice);
            Integer discountAmount = auctionPriceService.calculateDiscountAmount(auction, currentPrice);
            Long secondsUntilNextDrop = auctionPriceService.calculateSecondsUntilNextDrop(auction, calculatedStatus, now);
            Long displaySecondsUntilNextDrop = auctionPriceService.calculateDisplaySecondsUntilNextDrop(auction, calculatedStatus, now);

            Boolean canParticipate = auctionPriceService.canParticipate(calculatedStatus);
            AuctionButtonStatus buttonStatus = auctionPriceService.calculateButtonStatus(calculatedStatus);

            AuctionPriceStreamResponse response = AuctionPriceStreamResponse.of(
                    auction,
                    calculatedStatus,
                    now,
                    remainingUntilOpenSeconds,
                    remainingUntilCloseSeconds,
                    currentPrice,
                    nextPrice,
                    discountAmount,
                    secondsUntilNextDrop,
                    displaySecondsUntilNextDrop,
                    canParticipate,
                    buttonStatus
            );

            auctionSseService.send(auction.getId(), response);
        }
    }
}
