package com.t1.popcon.user.dto.statistics;

public record UserActivityStatisticsResponse(
    long ticketCount,
    long totalDrawCount,
    long totalAuctionCount,
    long reviewCount,
    long wonDrawCount,
    long lostDrawCount,
    long ongoingDrawCount,
    long waitingResultDrawCount,
    long wonAuctionCount,
    long likedPopupCount
) {}
