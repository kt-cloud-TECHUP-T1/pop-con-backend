package com.t1.popcon.queue.config;

import com.t1.popcon.queue.common.redis.QueueActiveRepository;
import com.t1.popcon.queue.common.redis.QueuePhaseScanner;
import com.t1.popcon.queue.common.redis.QueueWaitingRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 대기열 사이즈 게이지 — 부하테스트 모니터링용
 * - 5초 주기로 활성 phase를 스캔하여 waiting/active 인원 수를 Gauge로 expose
 * - 새 phase가 발견될 때 동적으로 Gauge 등록 (phaseType + phase_id 태그)
 */
@Component
@RequiredArgsConstructor
public class QueueSizeMetrics {

    private final QueueWaitingRepository waitingRepository;
    private final QueueActiveRepository activeRepository;
    private final QueuePhaseScanner phaseScanner;
    private final MeterRegistry registry;

    private final Map<String, AtomicLong> waitingCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> activeCounts = new ConcurrentHashMap<>();

    @Scheduled(fixedRate = 5000)
    public void updateQueueSizeMetrics() {
        Set<QueuePhaseScanner.PhaseKey> phases = phaseScanner.scanActivePhases();
        for (QueuePhaseScanner.PhaseKey phase : phases) {
            String key = phase.phaseType() + ":" + phase.phaseId();

            AtomicLong waiting = waitingCounts.computeIfAbsent(key, k -> {
                AtomicLong gauge = new AtomicLong(0);
                Gauge.builder("popcon_queue_waiting_size", gauge, AtomicLong::get)
                        .tag("phase", phase.phaseType())
                        .tag("phase_id", String.valueOf(phase.phaseId()))
                        .description("대기열 WAITING 인원 수")
                        .register(registry);
                return gauge;
            });
            waiting.set(waitingRepository.getWaitingCount(phase.phaseType(), phase.phaseId()));

            AtomicLong active = activeCounts.computeIfAbsent(key, k -> {
                AtomicLong gauge = new AtomicLong(0);
                Gauge.builder("popcon_queue_active_size", gauge, AtomicLong::get)
                        .tag("phase", phase.phaseType())
                        .tag("phase_id", String.valueOf(phase.phaseId()))
                        .description("대기열 ACTIVE 인원 수")
                        .register(registry);
                return gauge;
            });
            active.set(activeRepository.getActiveCount(phase.phaseType(), phase.phaseId()));
        }
    }
}
