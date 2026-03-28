package com.t1.popcon.queue.common.redis;

import lombok.RequiredArgsConstructor;
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

    /** 차단 만료 시각 조회 (epoch seconds, empty면 차단 아님) */
    public Optional<Long> getBlockedUntilEpoch(String phaseType, long phaseId, long userId) {
        String value = redisTemplate.opsForValue().get(QueueRedisKeys.block(phaseType, phaseId, userId));
        if (value == null) return Optional.empty();
        return Optional.of(Long.parseLong(value));
    }

    /** 차단 여부 조회 */
    public boolean isBlocked(String phaseType, long phaseId, long userId) {
        return Boolean.TRUE.equals(
            redisTemplate.hasKey(QueueRedisKeys.block(phaseType, phaseId, userId))
        );
    }
}
