package com.t1.popcon.queue.service;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.queue.common.config.QueueProperties;
import com.t1.popcon.queue.common.domain.PhaseType;
import com.t1.popcon.queue.common.domain.QueueStatus;
import com.t1.popcon.queue.common.redis.QueueActiveRepository;
import com.t1.popcon.queue.common.redis.QueueBlockRepository;
import com.t1.popcon.queue.common.redis.QueueRedisKeys;
import com.t1.popcon.queue.common.redis.QueueWaitingRepository;
import com.t1.popcon.queue.dto.response.QueueBlockedResponse;
import com.t1.popcon.queue.dto.response.QueueEntryResponse;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 대기열 진입 비즈니스 로직
 * - 차단 확인 → 기존 데이터 정리 → 분산 락 내 원자적 입장/대기 분기
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueueEntryService {

    private static final String LOCK_KEY_PREFIX = "queue:lock:";
    private static final long LOCK_WAIT_SECONDS = 5;
    private static final long LOCK_LEASE_SECONDS = 3;

    private final QueueWaitingRepository waitingRepository;
    private final QueueActiveRepository activeRepository;
    private final QueueBlockRepository blockRepository;
    private final QueueProperties properties;
    private final RedissonClient redissonClient;
    private final MeterRegistry registry;

    /**
     * 대기열 진입 처리
     * 1. 차단(BLOCKED) 여부 확인
     * 2. 기존 데이터 무조건 정리 (중복 진입 대비)
     * 3. 분산 락 내 ACTIVE 인원 확인 → 즉시 입장 or 대기열 등록
     */
    public QueueEntryResponse enter(PhaseType phaseType, long phaseId, long userId) {
        String type = phaseType.getValue();
        long activeTtl = properties.getActiveTtlSeconds(phaseType);

        // 1. 차단 여부 확인
        checkBlocked(type, phaseId, userId);

        // 2. 기존 데이터 정리 (재진입 시 이전 세션 제거 — 실패해도 진입은 진행)
        cleanupExistingEntry(type, phaseId, userId);

        // 3. 분산 락 기반 원자적 진입
        String lockKey = LOCK_KEY_PREFIX + type + ":" + phaseId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (!lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS)) {
                log.error("[Queue] 분산 락 획득 실패 - phaseType={}, phaseId={}", type, phaseId);
                throw new CustomException(ErrorCode.ERROR_SYSTEM);
            }

            // 만료된 ACTIVE 사용자 선제거 (여유 슬롯 확보)
            activeRepository.removeExpiredActive(type, phaseId);

            long activeCount = activeRepository.getActiveCount(type, phaseId);
            String queueToken = UUID.randomUUID().toString();

            if (activeCount < properties.getMaxActiveUsers()) {
                return enterActive(type, phaseId, userId, queueToken, activeTtl);
            } else {
                return enterWaiting(type, phaseId, userId, queueToken, activeTtl);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[Queue] 분산 락 인터럽트 - phaseType={}, phaseId={}", type, phaseId);
            throw new CustomException(ErrorCode.ERROR_SYSTEM);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    // ── 내부 메서드 ────────────────────────────────────────────

    /** 차단 여부 확인 — 차단 시 BLOCKED 응답과 함께 예외 */
    private void checkBlocked(String phaseType, long phaseId, long userId) {
        Optional<Long> blockedUntilEpoch = blockRepository.getBlockedUntilEpoch(phaseType, phaseId, userId);
        if (blockedUntilEpoch.isPresent()) {
            String blockedUntil = Instant.ofEpochSecond(blockedUntilEpoch.get())
                .atZone(ZoneId.of("Asia/Seoul"))
                .toLocalDateTime()
                .toString();
            log.info("[Queue] 차단된 사용자 진입 시도 - phaseType={}, phaseId={}, userId={}",
                phaseType, phaseId, userId % 1000);
            registry.counter("popcon_queue_enter_total",
                    "phase", phaseType, "phase_id", String.valueOf(phaseId), "result", "blocked")
                    .increment();
            throw new CustomException(ErrorCode.QUEUE_BLOCKED,
                new QueueBlockedResponse("BLOCKED", blockedUntil));
        }
    }

    /**
     * 기존 데이터 무조건 정리 (중복 진입 대비)
     * - 설계서: "조건 분기 없이 항상 정리 후 신규 진입 처리"
     * - ZREM은 대상 없어도 0 반환 (무해)
     * - 정리 실패 시 WARN 로그 후 진입 계속 (best-effort)
     */
    private void cleanupExistingEntry(String phaseType, long phaseId, long userId) {
        try {
            // 기존 queueToken 해시값 조회 (user HASH에 SHA-256 해시로 저장되어 있음)
            Map<Object, Object> userHash = activeRepository.getUserHash(phaseType, phaseId, userId);
            String existingTokenHash = null;
            if (!userHash.isEmpty()) {
                Object tokenObj = userHash.get(QueueRedisKeys.FIELD_QUEUE_TOKEN);
                existingTokenHash = tokenObj != null ? tokenObj.toString() : null;
            }

            waitingRepository.removeFromWaiting(phaseType, phaseId, userId);
            waitingRepository.removeFromHeartbeat(phaseType, phaseId, userId);
            activeRepository.removeFromActive(phaseType, phaseId, userId);
            activeRepository.deleteUserHash(phaseType, phaseId, userId);
            // user HASH에서 읽은 값은 이미 hash된 값 → ByHash 호출 (이중 해시 방지)
            if (existingTokenHash != null) {
                activeRepository.deleteQueueTokenByHash(existingTokenHash);
            }
        } catch (Exception e) {
            // 기존 데이터 정리 실패해도 신규 진입은 계속 진행 (TTL 만료 시 자동 정리)
            log.warn("[Queue] 기존 데이터 정리 실패 (계속 진행) - phaseType={}, phaseId={}, userId={}",
                phaseType, phaseId, userId % 1000, e);
        }
    }

    /** 즉시 입장 (ACTIVE) 처리 */
    private QueueEntryResponse enterActive(String phaseType, long phaseId, long userId,
                                            String queueToken, long activeTtlSeconds) {
        long expireAtMillis = Instant.now().plusSeconds(activeTtlSeconds).toEpochMilli();

        activeRepository.addToActive(phaseType, phaseId, userId, expireAtMillis);
        activeRepository.saveUserStatus(phaseType, phaseId, userId,
            QueueStatus.ACTIVE.name(), queueToken, activeTtlSeconds);
        activeRepository.saveQueueToken(queueToken, phaseType, phaseId, userId, activeTtlSeconds);

        log.info("[Queue] 즉시 입장 - phaseType={}, phaseId={}, userId={}",
            phaseType, phaseId, userId % 1000);
        registry.counter("popcon_queue_enter_total",
                "phase", phaseType, "phase_id", String.valueOf(phaseId), "result", "active")
                .increment();
        return QueueEntryResponse.active(queueToken);
    }

    /** 대기열 등록 (WAITING) 처리 */
    private QueueEntryResponse enterWaiting(String phaseType, long phaseId, long userId,
                                             String queueToken, long activeTtlSeconds) {
        long seq = waitingRepository.nextSequence(phaseType, phaseId);

        waitingRepository.addToWaiting(phaseType, phaseId, userId, seq);
        long heartbeatExpireAt = Instant.now()
            .plusSeconds(properties.getHeartbeatTtlSeconds())
            .toEpochMilli();
        waitingRepository.updateHeartbeat(phaseType, phaseId, userId, heartbeatExpireAt);
        activeRepository.saveUserStatus(phaseType, phaseId, userId,
            QueueStatus.WAITING.name(), queueToken, activeTtlSeconds);
        activeRepository.saveQueueToken(queueToken, phaseType, phaseId, userId, activeTtlSeconds);

        // 대기 순위 조회 (0-based → 1-based 변환)
        Long rank = waitingRepository.getWaitingRank(phaseType, phaseId, userId);
        long position = rank != null ? rank + 1 : 1;
        long estimatedWaitSeconds = properties.estimateWaitSeconds(position);

        log.info("[Queue] 대기열 등록 - phaseType={}, phaseId={}, userId={}, position={}",
            phaseType, phaseId, userId % 1000, position);
        registry.counter("popcon_queue_enter_total",
                "phase", phaseType, "phase_id", String.valueOf(phaseId), "result", "waiting")
                .increment();
        return QueueEntryResponse.waiting(queueToken, position, estimatedWaitSeconds,
            properties.getPollingDefaultMs());
    }

}
