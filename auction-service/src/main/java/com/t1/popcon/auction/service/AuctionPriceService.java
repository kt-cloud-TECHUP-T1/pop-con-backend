package com.t1.popcon.auction.service;

import com.t1.popcon.auction.domain.Auction;
import com.t1.popcon.auction.domain.AuctionStatus;
import org.springframework.stereotype.Service;

import java.time.*;

@Service
public class AuctionPriceService {

    public int calculateCurrentPrice(Auction auction, LocalDateTime now) {
        if (auction.isSold()) {
            return auction.getMinimumPrice();
        }

        if (now.isBefore(auction.getOpenedAt())) {
            return auction.getStartPrice();
        }

        if (now.isAfter(auction.getClosedAt())) {
            return auction.getMinimumPrice();
        }

        long elapsedSeconds = Duration.between(auction.getOpenedAt(), now).getSeconds();
        long dropCount = elapsedSeconds / auction.getPriceDropIntervalSeconds();

        int calculatedPrice = auction.getStartPrice()
                - (int) (dropCount * auction.getPriceDropUnit());

        return Math.max(calculatedPrice, auction.getMinimumPrice());
    }

    public long calculateSecondsUntilNextDrop(Auction auction, LocalDateTime now) {
        if (auction.isSold()) {
            return 0L;
        }

        if (now.isBefore(auction.getOpenedAt())) {
            return Duration.between(now, auction.getOpenedAt()).getSeconds();
        }

        if (now.isAfter(auction.getClosedAt())) {
            return 0L;
        }

        long elapsedSeconds = Duration.between(auction.getOpenedAt(), now).getSeconds();
        long interval = auction.getPriceDropIntervalSeconds();
        long remainder = elapsedSeconds % interval;

        return remainder == 0 ? interval : interval - remainder;
    }

    public AuctionStatus calculateStatus(Auction auction, LocalDateTime now) {
        if (auction.isSold()) {
            return AuctionStatus.SOLD_OUT;
        }

        if (now.isBefore(auction.getOpenedAt())) {
            return AuctionStatus.SCHEDULED;
        }

        if (now.isAfter(auction.getClosedAt())) {
            return AuctionStatus.CLOSED;
        }

        return AuctionStatus.OPEN;
    }
}
