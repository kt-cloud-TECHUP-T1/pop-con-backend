package com.t1.popcon.draw.dto.response;

public record DrawStatisticsResponse(
    long totalCount,
    long wonCount,
    long lostCount,
    long ongoingCount,
    long waitingResultCount
) {}
