package com.t1.popcon.queue.common.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 대기열 운영 설정값
 * - application.yml의 queue.* 바인딩
 * - @Validated: 앱 시작 시 잘못된 설정값 즉시 실패 처리
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "queue")
public class QueueProperties {

    /** 클라이언트 폴링 권장 주기 (ms) — 기본 3,000ms / 3초 */
    @Positive
    private long pollingDefaultMs = 3_000;

    /** 최대 동시 활성 사용자 수 */
    @Positive
    private int maxActiveUsers = 1_000;

    /** 승격 스케줄러 실행 주기 (ms) — 기본 1,000ms / 1초 */
    @Positive
    private long releaseIntervalMs = 1_000;

    /** 1회 최대 승격 인원 */
    @Positive
    private int maxReleasePerCycle = 100;

    /** ACTIVE 만료 정리 스케줄러 주기 (ms) — 기본 60,000ms / 1분 */
    @Positive
    private long cleanupIntervalMs = 60_000;

    /** WAITING heartbeat 정리 스케줄러 주기 (ms) — 기본 10,000ms / 10초 */
    @Positive
    private long heartbeatCleanupIntervalMs = 10_000;

    /** WAITING heartbeat 유효 시간 (초) — 기본 15초 (폴링 주기 3초 × 5배), 이 시간 이상 갱신 없으면 이탈 처리 */
    @Positive
    private long heartbeatTtlSeconds = 15;

    /** 차단 유지 시간 (초) — 기본 1,800초 / 30분 */
    @Positive
    private long blockTtlSeconds = 1_800;

    /** Active TTL 설정 */
    @Valid
    private ActiveTtl activeTtl = new ActiveTtl();

    /**
     * heartbeat TTL이 폴링 주기보다 반드시 길어야 함
     * - 기본값 기준: 15,000ms > 3,000ms ✓
     * - Math.multiplyExact: long overflow 방어 (overflow 발생 시 false 반환 → 검증 실패)
     */
    @AssertTrue(message = "heartbeatTtlSeconds(ms 환산)는 pollingDefaultMs보다 커야 합니다.")
    public boolean isHeartbeatTtlGreaterThanPolling() {
        try {
            return Math.multiplyExact(heartbeatTtlSeconds, 1000L) > pollingDefaultMs;
        } catch (ArithmeticException e) {
            return false;
        }
    }

    @Getter
    @Setter
    public static class ActiveTtl {
        /** 드로우 ACTIVE 유지 시간 (초) — 기본 900초 / 15분 */
        @Positive
        private long drawSeconds = 900;

        /** 경매 ACTIVE 유지 시간 (초) — 기본 3,600초 / 1시간 */
        @Positive
        private long auctionSeconds = 3_600;
    }
}
