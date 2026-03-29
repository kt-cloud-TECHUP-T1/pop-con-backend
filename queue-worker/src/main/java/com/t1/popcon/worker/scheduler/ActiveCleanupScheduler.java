package com.t1.popcon.worker.scheduler;

import com.t1.popcon.queue.common.redis.QueuePhaseScanner;
import com.t1.popcon.queue.common.redis.QueuePhaseScanner.PhaseKey;
import com.t1.popcon.worker.service.WorkerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * ACTIVE 만료 정리 스케줄러
 * - 주기: queue.cleanup-interval-ms (기본 60초)
 * - 동작: Redis SCAN으로 활성 phase 탐색 → 만료된 ACTIVE 사용자 정리
 * - 분산 락: 멀티 인스턴스 환경에서 중복 실행 방지 (락 획득 실패 시 즉시 스킵)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActiveCleanupScheduler {

    private final QueuePhaseScanner phaseScanner;
    private final WorkerService workerService;
    private final RedissonClient redissonClient;

    private static final String LOCK_KEY = "lock:worker:active-cleanup";

    @Scheduled(fixedRateString = "${queue.cleanup-interval-ms:60000}")
    public void cleanupExpiredActive() {
        RLock lock = redissonClient.getLock(LOCK_KEY);
        // waitTime=0: 다른 인스턴스가 실행 중이면 즉시 스킵
        if (!lock.tryLock()) {
            return;
        }
        try {
            Set<PhaseKey> activePhases = phaseScanner.scanActivePhases();
            if (activePhases.isEmpty()) {
                return;
            }

            log.debug("[ActiveCleanupScheduler] 활성 phase 수: {}", activePhases.size());

            for (PhaseKey phase : activePhases) {
                try {
                    workerService.cleanupExpiredActive(phase.phaseType(), phase.phaseId());
                } catch (Exception e) {
                    log.error("[ActiveCleanupScheduler] 정리 실패 - phase={}", phase, e);
                }
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
