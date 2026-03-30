package com.t1.popcon.queue.dto.response;

/**
 * 차단 상태 응답 (BLOCKED)
 * - CustomException(QUEUE_BLOCKED, data) 형태로 반환
 * - blockedUntil: 차단 해제 시각 (ISO-8601)
 */
public record QueueBlockedResponse(
    String status,
    String blockedUntil
) {
}
