package com.t1.popcon.auction.bid.dto.response;

public record AuctionStatisticsResponse(
    long totalCount,
    long wonCount
) {}
