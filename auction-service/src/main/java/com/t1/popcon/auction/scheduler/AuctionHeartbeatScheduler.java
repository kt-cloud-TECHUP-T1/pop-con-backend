package com.t1.popcon.auction.scheduler;

import com.t1.popcon.auction.domain.Auction;
import com.t1.popcon.auction.domain.AuctionStatus;
import com.t1.popcon.auction.repository.AuctionRepository;
import com.t1.popcon.auction.service.AuctionSseService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuctionHeartbeatScheduler {

    private final AuctionRepository auctionRepository;
    private final AuctionSseService auctionSseService;

    @Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        List<Auction> auctions = auctionRepository.findAllByStatusIn(
                List.of(AuctionStatus.SCHEDULED, AuctionStatus.OPEN, AuctionStatus.SOLD_OUT)
        );

        for (Auction auction : auctions) {
            if (!auctionSseService.hasSubscribers(auction.getId())) {
                continue;
            }
            auctionSseService.sendHeartbeat(auction.getId());
        }
    }
}
