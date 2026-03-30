package com.t1.popcon.auction.service;

import com.t1.popcon.auction.domain.Auction;
import com.t1.popcon.auction.domain.AuctionButtonStatus;
import com.t1.popcon.auction.domain.AuctionStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class AuctionPriceService {

    public Integer calculateCurrentPrice(
            Auction auction,
            AuctionStatus auctionStatus,
            LocalDateTime now,
            Integer soldOutPrice,
            LocalDateTime restockAnchorAt
    ) {
        if (now.isBefore(auction.getOpenedAt())) {
            return null;
        }

        if (now.isAfter(auction.getClosedAt())) {
            return auction.getMinimumPrice();
        }

        if (auctionStatus == AuctionStatus.SOLD_OUT) {
            return soldOutPrice != null ? soldOutPrice : calculateTimeBasedPrice(auction, now);
        }

        if (restockAnchorAt != null && soldOutPrice != null && !now.isBefore(restockAnchorAt)) {
            return calculateAnchoredPrice(auction, soldOutPrice, restockAnchorAt, now);
        }

        return calculateTimeBasedPrice(auction, now);
    }

    public Integer calculateNextPrice(Auction auction, Integer currentPrice) {
        if (currentPrice == null) {
            return null;
        }

        int nextPrice = currentPrice - auction.getPriceDropUnit();
        return Math.max(nextPrice, auction.getMinimumPrice());
    }

    public Long calculateSecondsUntilNextDrop(
            Auction auction,
            AuctionStatus auctionStatus,
            LocalDateTime now,
            LocalDateTime restockAnchorAt
    ) {
        if (auctionStatus == AuctionStatus.SOLD_OUT) {
            return 0L;
        }

        if (now.isBefore(auction.getOpenedAt()) || now.isAfter(auction.getClosedAt())) {
            return 0L;
        }

        LocalDateTime baseTime = (auctionStatus == AuctionStatus.OPEN && restockAnchorAt != null && !now.isBefore(restockAnchorAt))
                ? restockAnchorAt
                : auction.getOpenedAt();

        long elapsedSeconds = Duration.between(baseTime, now).getSeconds();
        long interval = auction.getPriceDropIntervalSeconds();
        long remainder = elapsedSeconds % interval;

        return remainder == 0 ? interval : interval - remainder;
    }

    public Long calculateDisplaySecondsUntilNextDrop(
            Auction auction,
            AuctionStatus auctionStatus,
            LocalDateTime now,
            LocalDateTime restockAnchorAt
    ) {
        if (auctionStatus == AuctionStatus.SOLD_OUT) {
            return auction.getPriceDropIntervalSeconds().longValue();
        }

        return calculateSecondsUntilNextDrop(auction, auctionStatus, now, restockAnchorAt);
    }

    public Long calculateRemainingUntilOpenSeconds(Auction auction, LocalDateTime now) {
        if (!now.isBefore(auction.getOpenedAt())) {
            return 0L;
        }

        return Duration.between(now, auction.getOpenedAt()).getSeconds();
    }

    public Long calculateRemainingUntilCloseSeconds(Auction auction, LocalDateTime now) {
        if (now.isBefore(auction.getOpenedAt()) || now.isAfter(auction.getClosedAt())) {
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

    public AuctionStatus calculateStatus(Auction auction, LocalDateTime now, boolean hasAvailableStock) {
        if (now.isBefore(auction.getOpenedAt())) {
            return AuctionStatus.SCHEDULED;
        }

        if (now.isAfter(auction.getClosedAt())) {
            return AuctionStatus.CLOSED;
        }

        if (!hasAvailableStock) {
            return AuctionStatus.SOLD_OUT;
        }

        return AuctionStatus.OPEN;
    }

    public boolean isPriceDropBoundary(Auction auction, LocalDateTime now) {
        if (now.isBefore(auction.getOpenedAt()) || now.isAfter(auction.getClosedAt())) {
            return false;
        }

        return now.equals(getCurrentPriceDropBoundaryTime(auction, now));
    }

    public LocalDateTime getCurrentPriceDropBoundaryTime(Auction auction, LocalDateTime now) {
        long elapsedSeconds = Duration.between(auction.getOpenedAt(), now).getSeconds();
        long interval = auction.getPriceDropIntervalSeconds();
        long boundaryOffset = (elapsedSeconds / interval) * interval;
        return auction.getOpenedAt().plusSeconds(boundaryOffset);
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

    public Integer calculateTimeBasedPrice(Auction auction, LocalDateTime now) {
        long elapsedSeconds = Duration.between(auction.getOpenedAt(), now).getSeconds();
        long dropCount = elapsedSeconds / auction.getPriceDropIntervalSeconds();

        int calculatedPrice = auction.getStartPrice()
                - (int) (dropCount * auction.getPriceDropUnit());

        return Math.max(calculatedPrice, auction.getMinimumPrice());
    }

    private Integer calculateAnchoredPrice(Auction auction, Integer soldOutPrice, LocalDateTime anchorTime, LocalDateTime now) {
        long elapsedSeconds = Duration.between(anchorTime, now).getSeconds();
        long dropCount = Math.max(elapsedSeconds, 0L) / auction.getPriceDropIntervalSeconds();

        int calculatedPrice = soldOutPrice - (int) (dropCount * auction.getPriceDropUnit());
        return Math.max(calculatedPrice, auction.getMinimumPrice());
    }
}
