package com.t1.popcon.auction.dto.response;

import com.t1.popcon.auction.domain.Auction;
import com.t1.popcon.auction.domain.AuctionStatus;
import com.t1.popcon.auction.domain.AuctionButtonStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AuctionDetailResponse(
        Long auctionId,
        String auctionStatus,
        LocalDateTime serverTime,
        LocalDateTime auctionOpenAt,
        LocalDateTime auctionCloseAt,
        Long remainingUntilOpenSeconds,
        Long remainingUntilCloseSeconds,
        Integer startPrice,
        Integer minimumPrice,
        Integer currentPrice,
        Integer nextPrice,
        Integer discountAmount,
        Integer priceDropUnit,
        Integer priceDropIntervalSeconds,
        Long secondsUntilNextDrop,
        Integer maxPurchaseQuantityPerRound,
        Boolean canParticipate,
        String buttonStatus
) {
    public static AuctionDetailResponse of(
            Auction auction,
            AuctionStatus auctionStatus,
            LocalDateTime serverTime,
            Long remainingUntilOpenSeconds,
            Long remainingUntilCloseSeconds,
            Integer currentPrice,
            Integer nextPrice,
            Integer discountAmount,
            Long secondsUntilNextDrop,
            Integer maxPurchaseQuantityPerRound,
            Boolean canParticipate,
            AuctionButtonStatus buttonStatus
    ) {
        return AuctionDetailResponse.builder()
                .auctionId(auction.getId())
                .auctionStatus(auctionStatus.name())
                .serverTime(serverTime)
                .auctionOpenAt(auction.getOpenedAt())
                .auctionCloseAt(auction.getClosedAt())
                .remainingUntilOpenSeconds(remainingUntilOpenSeconds)
                .remainingUntilCloseSeconds(remainingUntilCloseSeconds)
                .startPrice(auction.getStartPrice())
                .minimumPrice(auction.getMinimumPrice())
                .currentPrice(currentPrice)
                .nextPrice(nextPrice)
                .discountAmount(discountAmount)
                .priceDropUnit(auction.getPriceDropUnit())
                .priceDropIntervalSeconds(auction.getPriceDropIntervalSeconds())
                .secondsUntilNextDrop(secondsUntilNextDrop)
                .maxPurchaseQuantityPerRound(maxPurchaseQuantityPerRound)
                .canParticipate(canParticipate)
                .buttonStatus(buttonStatus.name())
                .build();
    }
}