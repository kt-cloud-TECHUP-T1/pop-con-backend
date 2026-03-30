package com.t1.popcon.worker.service;

import com.t1.popcon.queue.common.config.QueueProperties;
import com.t1.popcon.queue.common.domain.PhaseType;
import com.t1.popcon.queue.common.domain.QueueStatus;
import com.t1.popcon.queue.common.redis.QueueActiveRepository;
import com.t1.popcon.queue.common.redis.QueueCleanupRepository;
import com.t1.popcon.queue.common.redis.QueueRedisKeys;
import com.t1.popcon.queue.common.redis.QueueWaitingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 대기열 Worker 핵심 비즈니스 로직
 * - 승격(promote): WAITING → ACTIVE 전환
 * - ACTIVE 만료 정리: TTL 초과 ACTIVE 사용자 제거
 * - Heartbeat 만료 정리: 폴링 중단 WAITING 사용자 제거
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerService {

    private final QueueWaitingRepository waitingRepository;
    private final QueueActiveRepository activeRepository;
    private final QueueCleanupRepository cleanupRepository;
    private final QueueProperties queueProperties;

    /**
     * WAITING → ACTIVE 승격
     * 1. 만료된 ACTIVE 제거 → 여유 슬롯 계산
     * 2. ZPOPMIN으로 대기 상위 n명 원자적 추출
     * 3. heartbeat 만료 사용자 스킵 (슬롯 낭비 방지) → waiting 데이터 정리
     * 4. 유효 사용자: ACTIVE ZSET 추가 → 상태 변경 → heartbeat 제거 → TTL 갱신
     */
    public void promote(String phaseType, long phaseId) {
        // 1) 만료 ACTIVE 사용자 먼저 정리하여 슬롯 확보
        long expiredCount = activeRepository.removeExpiredActive(phaseType, phaseId);
        if (expiredCount > 0) {
            log.info("[Worker] 만료 ACTIVE 제거 - phaseType={}, phaseId={}, count={}",
                phaseType, phaseId, expiredCount);
        }

        // 2) 여유 슬롯 계산
        long activeCount = activeRepository.getActiveCount(phaseType, phaseId);
        long availableSlots = queueProperties.getMaxActiveUsers() - activeCount;
        if (availableSlots <= 0) {
            return; // 슬롯 없음 — 다음 주기에 재시도
        }

        // 승격 인원 = min(여유 슬롯, 1회 최대 승격량)
        int promoteCount = (int) Math.min(availableSlots, queueProperties.getMaxReleasePerCycle());

        // 3) ZPOPMIN으로 대기 상위 n명 원자적 추출 (추출 즉시 waiting ZSET에서 제거됨)
        List<String> promotedUserIds = waitingRepository.pollWaitingUsers(phaseType, phaseId, promoteCount);
        if (promotedUserIds.isEmpty()) {
            return;
        }

        // ACTIVE TTL 결정 (phaseType별 상이)
        long activeTtlSeconds = resolveActiveTtl(phaseType);
        long expireAtMillis = Instant.now().plusSeconds(activeTtlSeconds).toEpochMilli();
        long now = Instant.now().toEpochMilli(); // heartbeat 만료 기준 시각 (루프 외부에서 1회 캡처)

        int promotedSuccessCount = 0; // heartbeat 만료 스킵·예외 제외 실제 승격 인원
        for (String userIdStr : promotedUserIds) {
            try {
                long userId = Long.parseLong(userIdStr);

                // heartbeat 만료 확인 — 폴링 중단 사용자를 ACTIVE로 승격하지 않음 (슬롯 낭비 방지)
                // ZPOPMIN이 이미 waiting에서 제거했으므로, 만료 판정 시 user 데이터 정리 후 스킵
                Long heartbeatScore = waitingRepository.getHeartbeatScore(phaseType, phaseId, userId);
                if (heartbeatScore == null || heartbeatScore < now) {
                    log.warn("[Worker] heartbeat 만료 사용자 승격 스킵 - phaseType={}, phaseId={}, userId={}",
                        phaseType, phaseId, userId % 1000);
                    cleanupRepository.cleanupWaitingUserData(phaseType, phaseId, userId, null);
                    continue;
                }

                promoteUser(phaseType, phaseId, userId, expireAtMillis, activeTtlSeconds);
                promotedSuccessCount++;
            } catch (NumberFormatException e) {
                log.error("[Worker] userId 파싱 실패 - phaseType={}, phaseId={}, raw={}",
                    phaseType, phaseId, userIdStr, e);
            } catch (Exception e) {
                log.error("[Worker] 승격 실패 - phaseType={}, phaseId={}, userId={}",
                    phaseType, phaseId, userIdStr, e);
            }
        }

        log.info("[Worker] 승격 완료 - phaseType={}, phaseId={}, promoted={}, active={}",
            phaseType, phaseId, promotedSuccessCount, activeRepository.getActiveCount(phaseType, phaseId));
    }

    /**
     * 개별 사용자 승격 처리
     * - ACTIVE ZSET 추가 → 상태 ACTIVE → heartbeat 제거 → TTL 갱신
     */
    private void promoteUser(String phaseType, long phaseId, long userId,
                              long expireAtMillis, long activeTtlSeconds) {
        // ACTIVE ZSET에 추가 (score = 만료 timestamp)
        activeRepository.addToActive(phaseType, phaseId, userId, expireAtMillis);

        // user HASH 상태 → ACTIVE
        activeRepository.updateUserStatus(phaseType, phaseId, userId, QueueStatus.ACTIVE.name());

        // heartbeat ZSET에서 제거 (ACTIVE 전환 후 heartbeat 불필요)
        waitingRepository.removeFromHeartbeat(phaseType, phaseId, userId);

        // user HASH에서 queueToken 해시값 조회 → ByHash TTL 갱신 (이중 해시 방지)
        Map<Object, Object> userHash = activeRepository.getUserHash(phaseType, phaseId, userId);
        String tokenHash = (userHash != null)
            ? (String) userHash.get(QueueRedisKeys.FIELD_QUEUE_TOKEN)
            : null;

        // user HASH + queueToken 키 TTL 동시 갱신 (ACTIVE 기준)
        activeRepository.refreshActiveTtlByHash(phaseType, phaseId, userId, tokenHash, activeTtlSeconds);
    }

    /**
     * 만료된 ACTIVE 사용자 정리
     * - ZREMRANGEBYSCORE로 일괄 삭제 (개별 userId 반환 없음)
     * - 사용자별 상세 정리(token/HASH)는 Redis TTL 만료로 자동 정리에 위임
     */
    public void cleanupExpiredActive(String phaseType, long phaseId) {
        long removed = activeRepository.removeExpiredActive(phaseType, phaseId);
        if (removed > 0) {
            log.info("[Worker] 만료 ACTIVE 정리 - phaseType={}, phaseId={}, removed={}",
                phaseType, phaseId, removed);
        }
    }

    /**
     * 만료된 Heartbeat WAITING 사용자 정리
     * - Lua로 원자적 조회+제거 → 각 사용자 cleanup 호출
     * - heartbeat 만료 = 폴링 중단으로 간주 → 대기열에서 퇴출
     */
    public void cleanupExpiredHeartbeats(String phaseType, long phaseId) {
        long now = Instant.now().toEpochMilli();
        Set<String> expiredUserIds = waitingRepository.removeExpiredHeartbeatUsers(phaseType, phaseId, now);
        if (expiredUserIds.isEmpty()) {
            return;
        }

        for (String userIdStr : expiredUserIds) {
            try {
                long userId = Long.parseLong(userIdStr);
                // WAITING 사용자 전체 정리 (heartbeat는 Lua에서 이미 제거됨 → waiting ZSET + token/HASH는 cleanup에서 처리)
                cleanupRepository.cleanupWaitingUserData(phaseType, phaseId, userId, null);
            } catch (NumberFormatException e) {
                log.error("[Worker] heartbeat 정리 userId 파싱 실패 - raw={}", userIdStr, e);
            } catch (Exception e) {
                log.error("[Worker] heartbeat 정리 실패 - phaseType={}, phaseId={}, userId={}",
                    phaseType, phaseId, userIdStr, e);
            }
        }

        log.info("[Worker] heartbeat 만료 정리 - phaseType={}, phaseId={}, removed={}",
            phaseType, phaseId, expiredUserIds.size());
    }

    /** phaseType별 ACTIVE TTL(초) 반환 */
    private long resolveActiveTtl(String phaseType) {
        PhaseType resolved = PhaseType.from(phaseType);
        return switch (resolved) {
            case DRAW -> queueProperties.getActiveTtl().getDrawSeconds();
            case AUCTION -> queueProperties.getActiveTtl().getAuctionSeconds();
        };
    }
}
