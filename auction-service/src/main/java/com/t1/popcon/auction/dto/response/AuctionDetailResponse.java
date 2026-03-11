package com.t1.popcon.auction.dto.response;

import com.t1.popcon.auction.domain.Auction;
import com.t1.popcon.auction.domain.AuctionStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AuctionDetailResponse(
        Long auctionId,
        String auctionStatus,
        Integer startPrice,
        Integer minimumPrice,
        Integer currentPrice,
        Integer nextPrice,
        Integer priceDropUnit,
        Integer priceDropIntervalSeconds,
        Long secondsUntilNextDrop,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        LocalDateTime serverTime
) {
    public static AuctionDetailResponse of(
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

        return AuctionDetailResponse.builder()
                .auctionId(auction.getId())
                .auctionStatus(auctionStatus.name())
                .startPrice(auction.getStartPrice())
                .minimumPrice(auction.getMinimumPrice())
                .currentPrice(currentPrice)
                .nextPrice(nextPrice)
                .priceDropUnit(auction.getPriceDropUnit())
                .priceDropIntervalSeconds(auction.getPriceDropIntervalSeconds())
                .secondsUntilNextDrop(secondsUntilNextDrop)
                .openedAt(auction.getOpenedAt())
                .closedAt(auction.getClosedAt())
                .serverTime(serverTime)
                .build();
    }
}
