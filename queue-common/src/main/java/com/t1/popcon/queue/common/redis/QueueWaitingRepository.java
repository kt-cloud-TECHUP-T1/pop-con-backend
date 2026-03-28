package com.t1.popcon.queue.common.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * 대기열 WAITING 관련 Redis 연산
 * - 순번 채번 (Sequence), 대기 목록 (Waiting ZSET), heartbeat (Heartbeat ZSET)
 */
@Repository
@RequiredArgsConstructor
public class QueueWaitingRepository {

    private final StringRedisTemplate redisTemplate;

    /**
     * ZPOPMIN Lua 스크립트
     * - ZPOPMIN은 [member1, score1, member2, score2, ...] 평탄화 배열 반환
     * - Lua 1-based 홀수 인덱스(1, 3, 5...) 위치가 member — 짝수 인덱스(2, 4, 6...)는 score
     * - Redisson 연동 시 popMin() API 불일치를 우회하기 위해 Lua 직접 호출
     */
    @SuppressWarnings("unchecked")
    private static final DefaultRedisScript<List> ZPOPMIN_SCRIPT = new DefaultRedisScript<>(
        "local r = redis.call('ZPOPMIN', KEYS[1], tonumber(ARGV[1])) " +
        "local m = {} " +
        "for i = 1, #r, 2 do m[#m+1] = r[i] end " +
        "return m",
        List.class
    );

    /**
     * 만료된 heartbeat 조회 + 제거 원자 Lua 스크립트
     * - ZRANGEBYSCORE: score <= now 인 멤버(만료된 사용자) 조회
     * - ZREMRANGEBYSCORE: 동일 범위 원자적 제거 (Lua 블록 내 단일 실행 — TOCTOU 없음)
     * - ZREM + unpack 방식 대신 ZREMRANGEBYSCORE 사용 → 멤버 수 제한 없음
     * - 반환값: 제거된 userId 문자열 목록
     */
    @SuppressWarnings("unchecked")
    private static final DefaultRedisScript<List> EXPIRE_HEARTBEAT_SCRIPT = new DefaultRedisScript<>(
        "local members = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', ARGV[1]) " +
        "redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', ARGV[1]) " +
        "return members",
        List.class
    );

    // ── Sequence ─────────────────────────────────────────────────

    /**
     * 대기 순번 채번 (INCR)
     * - null 반환 시 Redis 장애로 판단하여 즉시 예외 (순번 0 오염 방지)
     */
    public long nextSequence(String phaseType, long phaseId) {
        Long seq = redisTemplate.opsForValue().increment(QueueRedisKeys.seq(phaseType, phaseId));
        if (seq == null) {
            throw new IllegalStateException(
                "Redis INCR returned null - phaseType=" + phaseType + ", phaseId=" + phaseId);
        }
        return seq;
    }

    // ── Waiting ZSET ──────────────────────────────────────────────

    /** 대기 목록에 추가 (score = 채번된 순번) */
    public void addToWaiting(String phaseType, long phaseId, long userId, long score) {
        redisTemplate.opsForZSet().add(
            QueueRedisKeys.waiting(phaseType, phaseId),
            String.valueOf(userId),
            score
        );
    }

    /** 대기 목록에서 제거 */
    public void removeFromWaiting(String phaseType, long phaseId, long userId) {
        redisTemplate.opsForZSet().remove(
            QueueRedisKeys.waiting(phaseType, phaseId),
            String.valueOf(userId)
        );
    }

    /** 대기 순위 조회 (0-based, null이면 목록에 없음) */
    public Long getWaitingRank(String phaseType, long phaseId, long userId) {
        return redisTemplate.opsForZSet().rank(
            QueueRedisKeys.waiting(phaseType, phaseId),
            String.valueOf(userId)
        );
    }

    /**
     * 대기 목록에서 상위 n명 원자적 추출 및 제거 (승격용)
     * - Lua 스크립트로 ZPOPMIN 직접 호출 — 단일 명령으로 추출+제거 (원자적)
     * - range+remove 방식 대비 concurrent remove와의 경쟁 조건 제거
     */
    @SuppressWarnings("unchecked")
    public List<String> pollWaitingUsers(String phaseType, long phaseId, int count) {
        if (count <= 0) return List.of();
        String key = QueueRedisKeys.waiting(phaseType, phaseId);
        List<String> result = redisTemplate.execute(ZPOPMIN_SCRIPT, List.of(key), String.valueOf(count));
        return result != null ? result : List.of();
    }

    // ── Heartbeat ZSET ────────────────────────────────────────────

    /**
     * heartbeat 갱신 (score = 만료 timestamp epoch millis)
     * 이 메서드 단독으로는 user HASH / queue:token TTL이 갱신되지 않음
     * → 반드시 QueueActiveRepository.refreshWaitingTtl()과 함께 호출할 것 (heartbeat API)
     */
    public void updateHeartbeat(String phaseType, long phaseId, long userId, long expireAtMillis) {
        redisTemplate.opsForZSet().add(
            QueueRedisKeys.heartbeat(phaseType, phaseId),
            String.valueOf(userId),
            expireAtMillis
        );
    }

    /** heartbeat 제거 */
    public void removeFromHeartbeat(String phaseType, long phaseId, long userId) {
        redisTemplate.opsForZSet().remove(
            QueueRedisKeys.heartbeat(phaseType, phaseId),
            String.valueOf(userId)
        );
    }

    /**
     * 만료된 heartbeat 사용자 원자적 조회 + 제거
     * - Lua 스크립트로 ZRANGEBYSCORE + ZREMRANGEBYSCORE를 단일 실행 (TOCTOU 없음)
     * - now: 호출부에서 Instant.now().toEpochMilli()를 구해 전달
     * - 반환값: 제거된 userId 문자열 목록 (후속 cleanup 처리에 사용)
     */
    @SuppressWarnings("unchecked")
    public Set<String> removeExpiredHeartbeatUsers(String phaseType, long phaseId, long now) {
        String key = QueueRedisKeys.heartbeat(phaseType, phaseId);
        List<String> removed = redisTemplate.execute(EXPIRE_HEARTBEAT_SCRIPT, List.of(key), String.valueOf(now));
        return removed != null ? new HashSet<>(removed) : Set.of();
    }
}
