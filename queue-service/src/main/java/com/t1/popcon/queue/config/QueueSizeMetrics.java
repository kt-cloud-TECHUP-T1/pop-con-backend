package com.t1.popcon.queue.config;

import com.t1.popcon.queue.common.redis.QueueActiveRepository;
import com.t1.popcon.queue.common.redis.QueuePhaseScanner;
import com.t1.popcon.queue.common.redis.QueueWaitingRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 대기열 사이즈 게이지 — 부하테스트 모니터링용
 * - 지정된 주기(기본 5초)로 활성 phase를 스캔하여 waiting/active 인원 수를 Gauge로 expose
 * - 새 phase가 발견될 때 동적으로 Gauge 등록 (phaseType + phase_id 태그)
 * - 활성 상태가 끝난 phase는 주기적으로 정리하여 메모리 누수 방지
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueueSizeMetrics {

    private final QueueWaitingRepository waitingRepository;
    private final QueueActiveRepository activeRepository;
    private final QueuePhaseScanner phaseScanner;
    private final MeterRegistry registry;

    private final Map<String, AtomicLong> waitingCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> activeCounts = new ConcurrentHashMap<>();
    private final Map<String, Gauge> registeredWaitingGauges = new ConcurrentHashMap<>();
    private final Map<String, Gauge> registeredActiveGauges = new ConcurrentHashMap<>();

    @Scheduled(fixedRateString = "${queue.metrics.update-interval-ms:5000}")
    public void updateQueueSizeMetrics() {
        try {
            Set<QueuePhaseScanner.PhaseKey> phases = phaseScanner.scanActivePhases();
            
            Set<String> activeKeys = phases.stream()
                    .map(p -> p.phaseType() + ":" + p.phaseId())
                    .collect(Collectors.toSet());

            cleanupStaleMetrics(activeKeys);

            for (QueuePhaseScanner.PhaseKey phase : phases) {
                String key = phase.phaseType() + ":" + phase.phaseId();
                
                try {
                    AtomicLong waiting = waitingCounts.computeIfAbsent(key, k -> {
                        AtomicLong gaugeNum = new AtomicLong(0);
                        Gauge gauge = Gauge.builder("popcon_queue_waiting_size", gaugeNum, AtomicLong::get)
                                .tag("phase", phase.phaseType())
                                .tag("phase_id", String.valueOf(phase.phaseId()))
                                .description("대기열 WAITING 인원 수")
                                .register(registry);
                        registeredWaitingGauges.put(k, gauge);
                        return gaugeNum;
                    });
                    waiting.set(waitingRepository.getWaitingCount(phase.phaseType(), phase.phaseId()));
                    
                } catch (Exception e) {
                    log.error("Failed to update waiting queue size metric for phase {}:{}", phase.phaseType(), phase.phaseId(), e);
                }

                try {
                    AtomicLong active = activeCounts.computeIfAbsent(key, k -> {
                        AtomicLong gaugeNum = new AtomicLong(0);
                        Gauge gauge = Gauge.builder("popcon_queue_active_size", gaugeNum, AtomicLong::get)
                                .tag("phase", phase.phaseType())
                                .tag("phase_id", String.valueOf(phase.phaseId()))
                                .description("대기열 ACTIVE 인원 수")
                                .register(registry);
                        registeredActiveGauges.put(k, gauge);
                        return gaugeNum;
                    });
                    active.set(activeRepository.getActiveCount(phase.phaseType(), phase.phaseId()));
                    
                } catch (Exception e) {
                    log.error("Failed to update active queue size metric for phase {}:{}", phase.phaseType(), phase.phaseId(), e);
                }
            }
        } catch (Exception e) {
            log.error("updateQueueSizeMetrics failed", e);
        }
    }

    private void cleanupStaleMetrics(Set<String> activeKeys) {
        waitingCounts.keySet().removeIf(key -> {
            if (!activeKeys.contains(key)) {
                Gauge gauge = registeredWaitingGauges.remove(key);
                if (gauge != null) {
                    registry.remove(gauge.getId());
                }
                return true;
            }
            return false;
        });

        activeCounts.keySet().removeIf(key -> {
            if (!activeKeys.contains(key)) {
                Gauge gauge = registeredActiveGauges.remove(key);
                if (gauge != null) {
                    registry.remove(gauge.getId());
                }
                return true;
            }
            return false;
        });
    }
}
