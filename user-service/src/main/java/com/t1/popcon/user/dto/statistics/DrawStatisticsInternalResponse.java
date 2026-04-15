package com.t1.popcon.user.dto.statistics;

public record DrawStatisticsInternalResponse(
    long totalCount,
    long wonCount,
    long lostCount,
    long ongoingCount,
    long waitingResultCount
) {}
