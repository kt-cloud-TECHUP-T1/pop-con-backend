package com.t1.popcon.draw.dto.response;

import lombok.Builder;

@Builder
public record DrawExecuteResponse(
    Long drawId,
    Long drawOptionId,
    int appliedCount,
    int winnerCount,
    int failedCount
) {
}
