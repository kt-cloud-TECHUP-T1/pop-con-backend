package com.t1.popcon.queue.common.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

/**
 * Anti-Macro 서비스가 저장한 Redis 점수 조회
 * - anti-macro-service와 동일한 Redis (또는 공유 저장소) 접근 필요
 * - 키 패턴: score:{identityKey} (identityKey는 userId 또는 visitorId)
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class AntiMacroScoreRepository {

    private final StringRedisTemplate redisTemplate;
    private static final String KEY_PREFIX = "score:";

    /**
     * userId로 합산 총점 조회
     * - Redis HASH의 모든 필드 값을 합산 (anti-macro-service의 getTotalScore 로직 재현)
     */
    public int getTotalScore(long userId) {
        String key = KEY_PREFIX + userId;
        Map<Object, Object> scores = redisTemplate.opsForHash().entries(key);

        if (scores.isEmpty()) {
            log.debug("[AntiMacro] 점수 기록 없음 - userId={}", userId);
            return 0;
        }

        return scores.values().stream()
                .mapToInt(val -> {
                    try {
                        return Integer.parseInt(val.toString());
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .sum();
    }
}
