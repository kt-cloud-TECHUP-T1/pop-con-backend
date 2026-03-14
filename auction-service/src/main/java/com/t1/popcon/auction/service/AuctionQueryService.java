package com.t1.popcon.auction.service;

import com.t1.popcon.auction.domain.Auction;
import com.t1.popcon.auction.domain.AuctionStatus;
import com.t1.popcon.auction.dto.response.AuctionDetailResponse;
import com.t1.popcon.auction.repository.AuctionRepository;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuctionQueryService {

    private static final int MAX_PURCHASE_QUANTITY_PER_ROUND = 10;

    private final AuctionRepository auctionRepository;
    private final AuctionPriceService auctionPriceService;

    public AuctionDetailResponse getAuctionDetail(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUCTION_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();

        AuctionStatus auctionStatus = auctionPriceService.calculateStatus(auction, now);
        Long remainingUntilOpenSeconds = auctionPriceService.calculateRemainingUntilOpenSeconds(auction, now);
        Long remainingUntilCloseSeconds = auctionPriceService.calculateRemainingUntilCloseSeconds(auction, now);

        Integer currentPrice = auctionPriceService.calculateCurrentPrice(auction, now);
        Integer nextPrice = auctionPriceService.calculateNextPrice(auction, currentPrice);
        Integer discountAmount = auctionPriceService.calculateDiscountAmount(auction, currentPrice);
        Long secondsUntilNextDrop = auctionPriceService.calculateSecondsUntilNextDrop(auction, now);

        return AuctionDetailResponse.of(
                auction,
                auctionStatus,
                now,
                remainingUntilOpenSeconds,
                remainingUntilCloseSeconds,
                currentPrice,
                nextPrice,
                discountAmount,
                secondsUntilNextDrop,
                MAX_PURCHASE_QUANTITY_PER_ROUND
        );
    }
}