package com.t1.popcon.queue.common.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * 대기열 차단 관련 Redis 연산
 * - 차단 등록/조회 (Block STRING)
 * - TTL = blockTtlSeconds, value = blockedUntil epoch seconds (응답용)
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class QueueBlockRepository {

    private final StringRedisTemplate redisTemplate;

    /**
     * 차단 등록
     * - value: 차단 만료 시각 (epoch seconds) — blockedUntil 응답용
     */
    public void block(String phaseType, long phaseId, long userId, long ttlSeconds) {
        long expireAt = Instant.now().plusSeconds(ttlSeconds).getEpochSecond();
        redisTemplate.opsForValue().set(
            QueueRedisKeys.block(phaseType, phaseId, userId),
            String.valueOf(expireAt),
            Duration.ofSeconds(ttlSeconds)
        );
    }

    /**
     * 차단 만료 시각 조회 (epoch seconds, empty면 차단 아님)
     * - Redis 데이터 오염 방어: 파싱 실패 시 WARN 로그 후 차단 없음으로 처리
     */
    public Optional<Long> getBlockedUntilEpoch(String phaseType, long phaseId, long userId) {
        String value = redisTemplate.opsForValue().get(QueueRedisKeys.block(phaseType, phaseId, userId));
        if (value == null) return Optional.empty();
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException e) {
            log.warn("[Queue] 차단 만료 시각 파싱 실패 (데이터 오염) - phaseType={}, phaseId={}, userId={}",
                phaseType, phaseId, userId % 1000, e);
            return Optional.empty();
        }
    }

    /** 차단 여부 조회 */
    public boolean isBlocked(String phaseType, long phaseId, long userId) {
        return Boolean.TRUE.equals(
            redisTemplate.hasKey(QueueRedisKeys.block(phaseType, phaseId, userId))
        );
    }
}
