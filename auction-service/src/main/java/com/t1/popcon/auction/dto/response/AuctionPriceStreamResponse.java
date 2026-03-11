package com.t1.popcon.auction.dto.response;

import com.t1.popcon.auction.domain.Auction;
import com.t1.popcon.auction.domain.AuctionStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AuctionPriceStreamResponse(
        Long auctionId,
        String auctionStatus,
        Integer currentPrice,
        Integer nextPrice,
        Long secondsUntilNextDrop,
        Integer priceDropUnit,
        Integer priceDropIntervalSeconds,
        LocalDateTime serverTime,
        LocalDateTime openedAt,
        LocalDateTime closedAt
) {
    public static AuctionPriceStreamResponse of(
            Auction auction,
            AuctionStatus auctionStatus,
            Integer currentPrice,
            Long secondsUntilNextDrop,
            LocalDateTime serverTime
    ) {
        int nextPrice = Math.max(
                currentPrice - auction.getPriceDropUnit(),
                auction.getMinimumPrice()
        );

        return AuctionPriceStreamResponse.builder()
                .auctionId(auction.getId())
                .auctionStatus(auctionStatus.name())
                .currentPrice(currentPrice)
                .nextPrice(nextPrice)
                .secondsUntilNextDrop(secondsUntilNextDrop)
                .priceDropUnit(auction.getPriceDropUnit())
                .priceDropIntervalSeconds(auction.getPriceDropIntervalSeconds())
                .serverTime(serverTime)
                .openedAt(auction.getOpenedAt())
                .closedAt(auction.getClosedAt())
                .build();
    }
}
