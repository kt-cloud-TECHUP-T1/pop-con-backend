package com.t1.popcon.auction.scheduler;

import com.t1.popcon.auction.domain.Auction;
import com.t1.popcon.auction.domain.AuctionStatus;
import com.t1.popcon.auction.dto.response.AuctionPriceStreamResponse;
import com.t1.popcon.auction.repository.AuctionRepository;
import com.t1.popcon.auction.service.AuctionPriceService;
import com.t1.popcon.auction.service.AuctionSseService;
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
    private final AuctionSseService auctionSseService;

    @Transactional
    @Scheduled(fixedRate = 1000)
    public void broadcastAuctionPrices() {
        LocalDateTime now = LocalDateTime.now();

        List<Auction> auctions = auctionRepository.findAllByStatusIn(
                List.of(AuctionStatus.SCHEDULED, AuctionStatus.OPEN)
        );

        for (Auction auction : auctions) {
            AuctionStatus calculatedStatus = auctionPriceService.calculateStatus(auction, now);

            if (auction.getStatus() != calculatedStatus) {
                auction.updateStatus(calculatedStatus);
            }

            if (!auctionSseService.hasSubscribers(auction.getId())) {
                continue;
            }

            int currentPrice = auctionPriceService.calculateCurrentPrice(auction, now);
            long secondsUntilNextDrop = auctionPriceService.calculateSecondsUntilNextDrop(auction, now);

            AuctionPriceStreamResponse response = AuctionPriceStreamResponse.of(
                    auction,
                    calculatedStatus,
                    currentPrice,
                    secondsUntilNextDrop,
                    now
            );

            auctionSseService.send(auction.getId(), response);
        }
    }
}