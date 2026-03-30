package com.t1.popcon.queue.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 대기열 진입 API 응답
 * - ACTIVE: status + queueToken
 * - WAITING: status + queueToken + position + estimatedWaitSeconds + pollAfterMs
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QueueEntryResponse(
    String status,
    String queueToken,
    Long position,
    Long estimatedWaitSeconds,
    Long pollAfterMs
) {

    /** 즉시 입장 (ACTIVE) 응답 */
    public static QueueEntryResponse active(String queueToken) {
        return new QueueEntryResponse("ACTIVE", queueToken, null, null, null);
    }

    /** 대기열 등록 (WAITING) 응답 */
    public static QueueEntryResponse waiting(String queueToken, long position,
                                              long estimatedWaitSeconds, long pollAfterMs) {
        return new QueueEntryResponse("WAITING", queueToken, position, estimatedWaitSeconds, pollAfterMs);
    }
}
