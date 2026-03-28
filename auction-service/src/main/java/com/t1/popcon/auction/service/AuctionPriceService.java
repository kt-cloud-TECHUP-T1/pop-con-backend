package com.t1.popcon.auction.service;

import com.t1.popcon.auction.domain.Auction;
import com.t1.popcon.auction.domain.AuctionButtonStatus;
import com.t1.popcon.auction.domain.AuctionStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class AuctionPriceService {

    public Integer calculateCurrentPrice(Auction auction, LocalDateTime now) {
        if (auction.isSold()) {
            return auction.getMinimumPrice();
        }

        if (now.isBefore(auction.getOpenedAt())) {
            return null;
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

    public Integer calculateNextPrice(Auction auction, Integer currentPrice) {
        if (currentPrice == null) {
            return null;
        }

        int nextPrice = currentPrice - auction.getPriceDropUnit();
        return Math.max(nextPrice, auction.getMinimumPrice());
    }

    public Long calculateSecondsUntilNextDrop(Auction auction, LocalDateTime now) {
        if (auction.isSold()) {
            return 0L;
        }

        if (now.isBefore(auction.getOpenedAt())) {
            return 0L;
        }

        if (now.isAfter(auction.getClosedAt())) {
            return 0L;
        }

        long elapsedSeconds = Duration.between(auction.getOpenedAt(), now).getSeconds();
        long interval = auction.getPriceDropIntervalSeconds();
        long remainder = elapsedSeconds % interval;

        return remainder == 0 ? interval : interval - remainder;
    }

    public Long calculateDisplaySecondsUntilNextDrop(Auction auction, AuctionStatus auctionStatus, LocalDateTime now) {
        if (auctionStatus == AuctionStatus.SOLD_OUT) {
            return auction.getPriceDropIntervalSeconds().longValue();
        }

        return calculateSecondsUntilNextDrop(auction, now);
    }

    public Long calculateRemainingUntilOpenSeconds(Auction auction, LocalDateTime now) {
        if (!now.isBefore(auction.getOpenedAt())) {
            return 0L;
        }

        return Duration.between(now, auction.getOpenedAt()).getSeconds();
    }

    public Long calculateRemainingUntilCloseSeconds(Auction auction, LocalDateTime now) {
        if (auction.isSold()) {
            return 0L;
        }

        if (now.isBefore(auction.getOpenedAt())) {
            return 0L;
        }

        if (now.isAfter(auction.getClosedAt())) {
            return 0L;
        }

        return Duration.between(now, auction.getClosedAt()).getSeconds();
    }

    public Integer calculateDiscountAmount(Auction auction, Integer currentPrice) {
        if (currentPrice == null) {
            return null;
        }

        return auction.getStartPrice() - currentPrice;
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

    public boolean canParticipate(AuctionStatus auctionStatus) {
        return auctionStatus == AuctionStatus.OPEN;
    }

    public AuctionButtonStatus calculateButtonStatus(AuctionStatus auctionStatus) {
        return switch (auctionStatus) {
            case SCHEDULED -> AuctionButtonStatus.WAITING;
            case OPEN -> AuctionButtonStatus.ENABLED;
            case SOLD_OUT -> AuctionButtonStatus.SOLD_OUT;
            case CLOSED -> AuctionButtonStatus.ENDED;
        };
    }
}
