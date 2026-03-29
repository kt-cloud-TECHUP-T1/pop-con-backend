package com.t1.popcon.queue.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 대기열 상태 조회 (폴링) 응답
 * - WAITING: status + position + estimatedWaitSeconds + pollAfterMs
 * - ACTIVE: status만 반환 (입장 허가)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QueueStatusResponse(
    String status,
    Long position,
    Long estimatedWaitSeconds,
    Long pollAfterMs
) {

    /** ACTIVE 상태 응답 */
    public static QueueStatusResponse active() {
        return new QueueStatusResponse("ACTIVE", null, null, null);
    }

    /** WAITING 상태 응답 (대기 순번 + 예상 대기 시간 포함) */
    public static QueueStatusResponse waiting(long position, long estimatedWaitSeconds, long pollAfterMs) {
        return new QueueStatusResponse("WAITING", position, estimatedWaitSeconds, pollAfterMs);
    }
}
