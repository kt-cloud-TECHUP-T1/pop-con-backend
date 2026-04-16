package com.t1.popcon.queue.service;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.queue.common.config.QueueProperties;
import com.t1.popcon.queue.common.domain.PhaseType;
import com.t1.popcon.queue.common.domain.QueueStatus;
import com.t1.popcon.queue.common.redis.QueueActiveRepository;
import com.t1.popcon.queue.common.redis.QueueCleanupRepository;
import com.t1.popcon.queue.common.redis.QueueWaitingRepository;
import com.t1.popcon.queue.dto.response.QueueStatusResponse;
import com.t1.popcon.queue.service.QueueTokenResolver.TokenInfo;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * 대기열 상태 조회(폴링) 및 자진 이탈 비즈니스 로직
 * - getStatus: queueToken 역조회 → 상태별 응답 + heartbeat 갱신
 * - leave: queueToken 역조회 → WAITING 데이터 정리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueuePollingService {

    private final QueueWaitingRepository waitingRepository;
    private final QueueActiveRepository activeRepository;
    private final QueueCleanupRepository cleanupRepository;
    private final QueueProperties properties;
    private final QueueTokenResolver tokenResolver;
    private final MeterRegistry registry;

    /**
     * 대기열 상태 조회
     * - WAITING: heartbeat 갱신 + user/token TTL 연장 + 순위 반환
     * - ACTIVE: 입장 허가 응답
     */
    public QueueStatusResponse getStatus(String queueToken) {
        TokenInfo info = tokenResolver.resolve(queueToken);

        Optional<String> statusOpt = activeRepository.getUserStatus(
            info.phaseType(), info.phaseId(), info.userId());
        if (statusOpt.isEmpty()) {
            log.warn("[Queue] 유저 상태 없음 - phaseType={}, phaseId={}, userId={}",
                info.phaseType(), info.phaseId(), info.userId() % 1000);
            throw new CustomException(ErrorCode.QUEUE_TOKEN_INVALID);
        }

        String status = statusOpt.get();

        if (QueueStatus.ACTIVE.name().equals(status)) {
            return QueueStatusResponse.active();
        }

        if (QueueStatus.WAITING.name().equals(status)) {
            return handleWaitingStatus(info, queueToken);
        }

        // BLOCKED 등 예상치 못한 상태 — Redis 데이터 오염으로 간주, 토큰 무효 처리
        log.warn("[Queue] 예상치 못한 상태 - status={}, phaseType={}, phaseId={}, userId={}",
            status, info.phaseType(), info.phaseId(), info.userId() % 1000);
        throw new CustomException(ErrorCode.QUEUE_TOKEN_INVALID);
    }

    /**
     * 대기열 자진 이탈
     * - WAITING 상태인 사용자만 이탈 가능
     * - ACTIVE 상태는 도메인 API 완료 시 정리
     */
    public void leave(String queueToken) {
        TokenInfo info = tokenResolver.resolve(queueToken);

        // 상태 확인 — WAITING만 자진 이탈 가능
        Optional<String> statusOpt = activeRepository.getUserStatus(
            info.phaseType(), info.phaseId(), info.userId());
        if (statusOpt.isEmpty() || !QueueStatus.WAITING.name().equals(statusOpt.get())) {
            log.warn("[Queue] 이탈 불가 상태 - status={}, userId={}",
                statusOpt.orElse("N/A"), info.userId() % 1000);
            throw new CustomException(ErrorCode.QUEUE_TOKEN_INVALID);
        }

        cleanupRepository.cleanupWaitingUserData(
            info.phaseType(), info.phaseId(), info.userId(), queueToken);
        log.info("[Queue] 자진 이탈 완료 - phaseType={}, phaseId={}, userId={}",
            info.phaseType(), info.phaseId(), info.userId() % 1000);
        registry.counter("popcon_queue_leave_total",
                "phase", info.phaseType(), "phase_id", String.valueOf(info.phaseId()),
                "reason", "voluntary")
                .increment();
    }

    // ── 내부 메서드 ────────────────────────────────────────────

    /** WAITING 상태 폴링 처리 — heartbeat 갱신 + 순위 응답 */
    private QueueStatusResponse handleWaitingStatus(TokenInfo info, String queueToken) {
        String phaseType = info.phaseType();
        long phaseId = info.phaseId();
        long userId = info.userId();

        // heartbeat 갱신 (WAITING 생존 신호)
        long heartbeatExpireAt = Instant.now()
            .plusSeconds(properties.getHeartbeatTtlSeconds())
            .toEpochMilli();
        waitingRepository.updateHeartbeat(phaseType, phaseId, userId, heartbeatExpireAt);

        // user HASH / token TTL 연장 (키 만료 방지)
        long activeTtl = getActiveTtlByPhaseType(phaseType);
        activeRepository.refreshWaitingTtl(phaseType, phaseId, userId, queueToken, activeTtl);

        // 대기 순위 조회
        Long rank = waitingRepository.getWaitingRank(phaseType, phaseId, userId);
        if (rank == null) {
            // WAITING 상태인데 ZSET에 없음 — 승격 직후일 수 있음 (스케줄러가 상태 변경 전)
            log.debug("[Queue] WAITING 상태이나 대기 목록에 없음 (승격 추정) - userId={}", userId % 1000);
            return QueueStatusResponse.active();
        }

        long position = rank + 1;
        long estimatedWaitSeconds = properties.estimateWaitSeconds(position);
        return QueueStatusResponse.waiting(position, estimatedWaitSeconds,
            properties.getPollingDefaultMs());
    }

    /** phaseType 문자열 기반 ACTIVE TTL 반환 (초) — QueueProperties 위임 */
    private long getActiveTtlByPhaseType(String phaseType) {
        return properties.getActiveTtlSeconds(PhaseType.from(phaseType));
    }
}
