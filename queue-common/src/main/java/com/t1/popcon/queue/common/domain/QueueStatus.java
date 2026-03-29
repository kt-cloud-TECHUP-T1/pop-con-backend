package com.t1.popcon.queue.common.domain;

/**
 * 대기열 사용자 상태
 * - WAITING : 대기 중 (폴링 구간)
 * - ACTIVE  : 입장 허가 (퀴즈~결제 완료 구간)
 * - BLOCKED : 정책 위반으로 차단 (보안 퀴즈 3회 실패 등)
 */
public enum QueueStatus {
    WAITING,
    ACTIVE,
    BLOCKED
}
