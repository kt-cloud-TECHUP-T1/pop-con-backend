package com.t1.popcon.queue.common.redis;

import com.t1.popcon.common.encryption.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * 대기열 ACTIVE 관련 Redis 연산
 * - 활성 목록 (Active ZSET), 유저 상태 (User HASH), 큐 토큰 (Token HASH), 퀴즈 통과 토큰 (QuizPassedToken HASH)
 * - token 관련 Redis 키는 raw token 노출 방지를 위해 EncryptionService.generateHash()로 SHA-256 변환 후 사용
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class QueueActiveRepository {

    private final StringRedisTemplate redisTemplate;
    /** raw token → SHA-256 해시 변환 (Redis 키 이름 보호용) */
    private final EncryptionService encryptionService;

    // ── Active ZSET ───────────────────────────────────────────────

    /** 활성 목록에 추가 (score = ACTIVE 만료 timestamp epoch millis) */
    public void addToActive(String phaseType, long phaseId, long userId, long expireAtMillis) {
        redisTemplate.opsForZSet().add(
            QueueRedisKeys.active(phaseType, phaseId),
            String.valueOf(userId),
            expireAtMillis
        );
    }

    /** 활성 목록에서 제거 */
    public void removeFromActive(String phaseType, long phaseId, long userId) {
        redisTemplate.opsForZSet().remove(
            QueueRedisKeys.active(phaseType, phaseId),
            String.valueOf(userId)
        );
    }

    /** 현재 활성 인원 조회 */
    public long getActiveCount(String phaseType, long phaseId) {
        Long count = redisTemplate.opsForZSet().zCard(QueueRedisKeys.active(phaseType, phaseId));
        return count != null ? count : 0L;
    }

    /** 만료된 ACTIVE 사용자 일괄 제거 (score < 현재 시각) */
    public long removeExpiredActive(String phaseType, long phaseId) {
        long now = Instant.now().toEpochMilli();
        Long removed = redisTemplate.opsForZSet().removeRangeByScore(
            QueueRedisKeys.active(phaseType, phaseId),
            Double.NEGATIVE_INFINITY,
            now
        );
        return removed != null ? removed : 0L;
    }

    /** 사용자의 ACTIVE 만료 시각 조회 (epoch millis, empty면 활성 아님) */
    public Optional<Long> getActiveExpireAt(String phaseType, long phaseId, long userId) {
        Double score = redisTemplate.opsForZSet().score(
            QueueRedisKeys.active(phaseType, phaseId),
            String.valueOf(userId)
        );
        return Optional.ofNullable(score).map(Double::longValue);
    }

    // ── User Status HASH ──────────────────────────────────────────

    /**
     * 유저 상태 및 queueToken 저장
     * - HSET + EXPIRE를 MULTI/EXEC로 원자적 처리 (TTL 없는 키 잔존 방지)
     * - ttlSeconds: 진입 시점 ACTIVE TTL (draw=900s / auction=3600s) — 안전망 만료
     */
    @SuppressWarnings("unchecked")
    public void saveUserStatus(String phaseType, long phaseId, long userId,
                                String status, String queueToken, long ttlSeconds) {
        String key = QueueRedisKeys.user(phaseType, phaseId, userId);
        Map<String, String> fields = Map.of(
            QueueRedisKeys.FIELD_STATUS, status,
            // user HASH에는 hash값만 저장 (raw token 노출 방지 — Redis MONITOR/HGETALL 대비)
            QueueRedisKeys.FIELD_QUEUE_TOKEN, hashToken(queueToken)
        );
        redisTemplate.execute(new SessionCallback<>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                operations.opsForHash().putAll(key, fields);
                operations.expire(key, Duration.ofSeconds(ttlSeconds));
                return operations.exec();
            }
        });
    }

    /**
     * WAITING heartbeat 갱신 시 user HASH 및 queueToken TTL 연장
     * - heartbeat ZSET 갱신(updateHeartbeat)과 함께 호출해야 키 만료 방지 가능
     * - ttlSeconds: activeTtl 기준 rolling window — 폴링이 살아있는 한 만료되지 않음
     * - queueToken이 null인 경우 token 키 TTL 갱신 생략
     */
    public void refreshWaitingTtl(String phaseType, long phaseId, long userId,
                                   String queueToken, long ttlSeconds) {
        Duration ttl = Duration.ofSeconds(ttlSeconds);
        expireWithWarn(QueueRedisKeys.user(phaseType, phaseId, userId), ttl, "user HASH (WAITING)");
        if (queueToken != null) {
            expireWithWarn(QueueRedisKeys.token(hashToken(queueToken)), ttl, "queueToken (WAITING)");
        }
    }

    /**
     * ACTIVE 승격 시 user HASH 및 queueToken TTL 갱신
     * - WAITING 진입 시 설정된 안전망 TTL을 실제 ACTIVE TTL로 재설정
     * - queueToken이 null인 경우 token 키 TTL 갱신 생략
     */
    public void refreshActiveTtl(String phaseType, long phaseId, long userId,
                                  String queueToken, long ttlSeconds) {
        Duration ttl = Duration.ofSeconds(ttlSeconds);
        expireWithWarn(QueueRedisKeys.user(phaseType, phaseId, userId), ttl, "user HASH (ACTIVE)");
        if (queueToken != null) {
            expireWithWarn(QueueRedisKeys.token(hashToken(queueToken)), ttl, "queueToken (ACTIVE)");
        }
    }

    /** 유저 상태 필드만 업데이트 */
    public void updateUserStatus(String phaseType, long phaseId, long userId, String status) {
        redisTemplate.opsForHash().put(
            QueueRedisKeys.user(phaseType, phaseId, userId),
            QueueRedisKeys.FIELD_STATUS,
            status
        );
    }

    /** 유저 상태 조회 (status 필드) */
    public Optional<String> getUserStatus(String phaseType, long phaseId, long userId) {
        Object value = redisTemplate.opsForHash().get(
            QueueRedisKeys.user(phaseType, phaseId, userId),
            QueueRedisKeys.FIELD_STATUS
        );
        return Optional.ofNullable(value).map(Object::toString);
    }

    /** 유저 상태 HASH 전체 조회 */
    public Map<Object, Object> getUserHash(String phaseType, long phaseId, long userId) {
        return redisTemplate.opsForHash().entries(QueueRedisKeys.user(phaseType, phaseId, userId));
    }

    /** 유저 상태 HASH 삭제 */
    public void deleteUserHash(String phaseType, long phaseId, long userId) {
        redisTemplate.delete(QueueRedisKeys.user(phaseType, phaseId, userId));
    }

    // ── Queue Token HASH ──────────────────────────────────────────

    /**
     * queueToken 저장
     * - HSET + EXPIRE를 MULTI/EXEC로 원자적 처리 (TTL 없는 키 잔존 방지)
     * - ttlSeconds: 진입 시점 ACTIVE TTL — user HASH와 동일한 만료 기준 적용
     */
    @SuppressWarnings("unchecked")
    public void saveQueueToken(String queueToken, String phaseType, long phaseId,
                                long userId, long ttlSeconds) {
        String key = QueueRedisKeys.token(hashToken(queueToken));
        Map<String, String> fields = Map.of(
            QueueRedisKeys.FIELD_PHASE_TYPE, phaseType,
            QueueRedisKeys.FIELD_PHASE_ID, String.valueOf(phaseId),
            QueueRedisKeys.FIELD_USER_ID, String.valueOf(userId)
        );
        redisTemplate.execute(new SessionCallback<>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                operations.opsForHash().putAll(key, fields);
                operations.expire(key, Duration.ofSeconds(ttlSeconds));
                return operations.exec();
            }
        });
    }

    /** queueToken HASH 전체 조회 */
    public Map<Object, Object> getQueueTokenHash(String queueToken) {
        return redisTemplate.opsForHash().entries(QueueRedisKeys.token(hashToken(queueToken)));
    }

    /** queueToken 삭제 (raw token 입력 → 내부에서 해시 후 삭제) */
    public void deleteQueueToken(String queueToken) {
        redisTemplate.delete(QueueRedisKeys.token(hashToken(queueToken)));
    }

    /**
     * queueToken 삭제 (user HASH에서 읽은 해시값 직접 입력)
     * - user HASH의 FIELD_QUEUE_TOKEN은 이미 hashToken() 처리된 값 → 이중 해시 방지
     */
    public void deleteQueueTokenByHash(String hashedToken) {
        redisTemplate.delete(QueueRedisKeys.token(hashedToken));
    }

    // ── Quiz Passed Token HASH ────────────────────────────────────

    /**
     * 퀴즈 통과 토큰을 user HASH에 기록
     * - cleanup 시 quizPassedToken 역참조용
     * - 퀴즈 통과 직후 saveQuizPassedToken과 함께 호출
     */
    public void saveQuizPassedTokenToUserHash(String phaseType, long phaseId,
                                              long userId, String quizPassedToken) {
        // user HASH에는 hash값만 저장 (raw token 노출 방지)
        redisTemplate.opsForHash().put(
            QueueRedisKeys.user(phaseType, phaseId, userId),
            QueueRedisKeys.FIELD_QUIZ_PASSED_TOKEN,
            hashToken(quizPassedToken)
        );
    }

    /**
     * 퀴즈 통과 토큰 저장
     * - HSET + EXPIRE를 MULTI/EXEC로 원자적 처리 (TTL 없는 키 잔존 방지)
     * - ttlSeconds: 발급 시점 기준 ACTIVE 잔여 시간
     */
    @SuppressWarnings("unchecked")
    public void saveQuizPassedToken(String token, String phaseType, long phaseId,
                                    long userId, long ttlSeconds) {
        String key = QueueRedisKeys.quizPassedToken(hashToken(token));
        Map<String, String> fields = Map.of(
            QueueRedisKeys.FIELD_PHASE_TYPE, phaseType,
            QueueRedisKeys.FIELD_PHASE_ID, String.valueOf(phaseId),
            QueueRedisKeys.FIELD_USER_ID, String.valueOf(userId)
        );
        redisTemplate.execute(new SessionCallback<>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                operations.opsForHash().putAll(key, fields);
                operations.expire(key, Duration.ofSeconds(ttlSeconds));
                return operations.exec();
            }
        });
    }

    /** 퀴즈 통과 토큰 HASH 전체 조회 */
    public Map<Object, Object> getQuizPassedTokenHash(String token) {
        return redisTemplate.opsForHash().entries(QueueRedisKeys.quizPassedToken(hashToken(token)));
    }

    /** 퀴즈 통과 토큰 삭제 (raw token 입력 → 내부에서 해시 후 삭제) */
    public void deleteQuizPassedToken(String token) {
        redisTemplate.delete(QueueRedisKeys.quizPassedToken(hashToken(token)));
    }

    /**
     * 퀴즈 통과 토큰 삭제 (user HASH에서 읽은 해시값 직접 입력)
     * - user HASH의 FIELD_QUIZ_PASSED_TOKEN은 이미 hashToken() 처리된 값 → 이중 해시 방지
     */
    public void deleteQuizPassedTokenByHash(String hashedToken) {
        redisTemplate.delete(QueueRedisKeys.quizPassedToken(hashedToken));
    }

    // ── 내부 유틸 ─────────────────────────────────────────────────

    /**
     * raw token → SHA-256 해시 (Redis 키 이름 보호)
     * - EncryptionService.generateHash() 위임 — 공통 해시 로직 재사용
     * - 결과값만 Redis 키 세그먼트로 사용, raw token은 HASH value에 저장하지 않음
     */
    private String hashToken(String rawToken) {
        return encryptionService.generateHash(rawToken);
    }

    /**
     * TTL 갱신 후 결과 검증
     * - false/null: 키 미존재 또는 연결 장애 → WARN 로그 (TTL 만료 시 자동 정리되므로 예외 미발생)
     */
    private void expireWithWarn(String key, Duration ttl, String context) {
        Boolean result = redisTemplate.expire(key, ttl);
        if (!Boolean.TRUE.equals(result)) {
            log.warn("[Queue] TTL 갱신 실패 — 키 없음 또는 장애 - key={}, context={}", key, context);
        }
    }
}
