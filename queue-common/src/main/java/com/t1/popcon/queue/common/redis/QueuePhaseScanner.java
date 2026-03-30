package com.t1.popcon.queue.common.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * 대기열 Redis 키 스캔 유틸리티
 * - queue:waiting:*, queue:active:*, queue:heartbeat:* 키 스캔하여 활성 phase 목록 확보
 * - 스케줄러에서 각 phase 별 작업 수행 시 활용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueuePhaseScanner {

    private final StringRedisTemplate redisTemplate;

    private static final String WAITING_PATTERN = "queue:waiting:*";
    private static final String ACTIVE_PATTERN = "queue:active:*";
    private static final String HEARTBEAT_PATTERN = "queue:heartbeat:*";

    /**
     * 활성 phase 목록 스캔 (phaseType + phaseId 파싱)
     * - queue:waiting:* / queue:active:* / queue:heartbeat:* 패턴 키 탐색
     * - 각 키에서 phaseType, phaseId 파싱 후 Set에 수집
     * - keyFormat: queue:{type}:{type}:{id} → type=draw/auction, id=phaseId
     */
    public Set<PhaseKey> scanActivePhases() {
        Set<PhaseKey> phases = new HashSet<>();
        phases.addAll(scanPhasesByPattern(WAITING_PATTERN));
        phases.addAll(scanPhasesByPattern(ACTIVE_PATTERN));
        phases.addAll(scanPhasesByPattern(HEARTBEAT_PATTERN));
        return phases;
    }

    /**
     * 단일 패턴으로 phase 목록 스캔
     * - SCAN 사용: Redis blocking 방지 (KEYS 명령은 O(N) 블로킹)
     * - batchSize: 100 (적절한 배치 크기로 반복 스캔)
     */
    private Set<PhaseKey> scanPhasesByPattern(String pattern) {
        Set<PhaseKey> phases = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(100)
                .build();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                PhaseKey phaseKey = parsePhaseKey(key);
                if (phaseKey != null) {
                    phases.add(phaseKey);
                }
            }
        } catch (Exception e) {
            log.error("[QueuePhaseScanner] Redis SCAN 실패 - pattern={}", pattern, e);
        }
        return phases;
    }

    /**
     * Redis 키에서 phaseType, phaseId 파싱
     * - keyFormat: queue:{target}:{phaseType}:{phaseId}
     * - 예: queue:waiting:draw:1 → phaseType=draw, phaseId=1
     */
    private PhaseKey parsePhaseKey(String key) {
        try {
            String[] parts = key.split(":");
            if (parts.length < 4) {
                log.warn("[QueuePhaseScanner] 잘못된 키 형식 - key={}", key);
                return null;
            }
            String phaseType = parts[2];
            long phaseId = Long.parseLong(parts[3]);
            return new PhaseKey(phaseType, phaseId);
        } catch (NumberFormatException e) {
            log.warn("[QueuePhaseScanner] phaseId 파싱 실패 - key={}", key, e);
            return null;
        }
    }

    /**
     * phaseType + phaseId 복합 키
     */
    public record PhaseKey(String phaseType, long phaseId) {
        @Override
        public String toString() {
            return phaseType + ":" + phaseId;
        }
    }
}
