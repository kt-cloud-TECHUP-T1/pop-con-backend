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
import java.util.concurrent.TimeUnit;

/**
 * 대기열 승격 스케줄러
 * - 주기: queue.release-interval-ms (기본 1초)
 * - 동작: Redis SCAN으로 활성 phase 탐색 → 각 phase에서 WAITING → ACTIVE 승격 처리
 * - 분산 락: 멀티 인스턴스 환경에서 중복 실행 방지 (락 획득 실패 시 즉시 스킵)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionScheduler {

    private final QueuePhaseScanner phaseScanner;
    private final WorkerService workerService;
    private final RedissonClient redissonClient;

    private static final String LOCK_KEY = "lock:worker:promotion";
    /** 승격 주기(1s)보다 충분히 길게 설정하여 정상 실행 도중 락 만료 방지 */
    private static final long LOCK_LEASE_SECONDS = 5;

    @Scheduled(fixedRateString = "${queue.release-interval-ms:1000}")
    public void promoteWaitingToActive() {
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

            log.debug("[PromotionScheduler] 활성 phase 수: {}", activePhases.size());

            for (PhaseKey phase : activePhases) {
                try {
                    workerService.promote(phase.phaseType(), phase.phaseId());
                } catch (Exception e) {
                    log.error("[PromotionScheduler] 승격 실패 - phase={}", phase, e);
                }
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
