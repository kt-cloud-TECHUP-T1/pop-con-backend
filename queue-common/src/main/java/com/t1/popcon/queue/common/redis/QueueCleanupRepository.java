package com.t1.popcon.queue.common.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Map;

/**
 * 대기열 데이터 정리 연산
 * - ACTIVE/WAITING 사용자 관련 모든 Redis 키 일괄 제거
 * - complete/block API, ACTIVE 만료 스케줄러, heartbeat 만료 스케줄러에서 호출
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class QueueCleanupRepository {

    private final QueueWaitingRepository waitingRepository;
    private final QueueActiveRepository activeRepository;

    /**
     * ACTIVE 사용자 관련 모든 Redis 데이터 정리
     * - token/quizPassedToken 삭제 → active ZSET 제거 → user HASH 삭제 순으로 처리
     * - token 삭제 실패 시 경고 후 계속 진행 (TTL 만료로 자동 정리됨)
     */
    public void cleanupUserData(String phaseType, long phaseId, long userId, String queueToken) {
        try {
            Map<Object, Object> userHash = activeRepository.getUserHash(phaseType, phaseId, userId);

            // quizPassedToken 역참조 삭제 — user HASH에 hash값으로 저장되어 있으므로 ByHash 호출
            Object quizPassedTokenHash = userHash.get(QueueRedisKeys.FIELD_QUIZ_PASSED_TOKEN);
            if (quizPassedTokenHash != null) {
                activeRepository.deleteQuizPassedTokenByHash(quizPassedTokenHash.toString());
            }

            // queueToken: user HASH에 hash값으로 저장 → ByHash 호출 (이중 해시 방지)
            // user HASH 없으면 파라미터(raw token)로 폴백 → deleteQueueToken(raw) 호출
            Object tokenHashFromHash = userHash.get(QueueRedisKeys.FIELD_QUEUE_TOKEN);
            if (tokenHashFromHash != null) {
                activeRepository.deleteQueueTokenByHash(tokenHashFromHash.toString());
            } else if (queueToken != null) {
                activeRepository.deleteQueueToken(queueToken);
            }
        } catch (Exception e) {
            // 토큰 삭제 실패 시 TTL 만료 후 자동 정리되므로 경고 후 계속 진행
            log.warn("[Queue] 토큰 삭제 실패 - phaseType={}, phaseId={}, userId={}",
                phaseType, phaseId, userId % 1000, e);
        }

        // 토큰 삭제 후 ZSET/HASH 정리 (역순으로 삭제해야 데이터 정합성 보장)
        activeRepository.removeFromActive(phaseType, phaseId, userId);
        activeRepository.deleteUserHash(phaseType, phaseId, userId);
        log.info("[Queue] 슬롯 회수 완료 - phaseType={}, phaseId={}, userId={}",
            phaseType, phaseId, userId % 1000);
    }

    /**
     * WAITING 사용자 관련 Redis 데이터 정리
     * - waiting/heartbeat ZSET 제거 → token 삭제 → user HASH 삭제 순으로 처리
     * - 자진 이탈 / heartbeat 만료 정리 시 호출
     */
    public void cleanupWaitingUserData(String phaseType, long phaseId, long userId, String queueToken) {
        waitingRepository.removeFromWaiting(phaseType, phaseId, userId);
        waitingRepository.removeFromHeartbeat(phaseType, phaseId, userId);

        // queueToken: user HASH 역참조 우선, 없으면 파라미터 폴백 (heartbeat 만료 정리 시 파라미터가 null일 수 있음)
        try {
            Map<Object, Object> userHash = activeRepository.getUserHash(phaseType, phaseId, userId);
            // user HASH에 hash값으로 저장 → ByHash 호출, 없으면 파라미터 raw token으로 폴백
            Object tokenHashFromHash = userHash.get(QueueRedisKeys.FIELD_QUEUE_TOKEN);
            if (tokenHashFromHash != null) {
                activeRepository.deleteQueueTokenByHash(tokenHashFromHash.toString());
            } else if (queueToken != null) {
                activeRepository.deleteQueueToken(queueToken);
            }
        } catch (Exception e) {
            log.warn("[Queue] WAITING 토큰 삭제 실패 - phaseType={}, phaseId={}, userId={}",
                phaseType, phaseId, userId % 1000, e);
        }

        activeRepository.deleteUserHash(phaseType, phaseId, userId);
        log.info("[Queue] WAITING 정리 완료 - phaseType={}, phaseId={}, userId={}",
            phaseType, phaseId, userId % 1000);
    }
}
