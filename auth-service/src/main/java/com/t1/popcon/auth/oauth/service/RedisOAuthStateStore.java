package com.t1.popcon.auth.oauth.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
/**
 * Redis 기반 OAuth state 저장 구현체
 *
 * key 구조:
 * oauth:state:{provider}:{state}
 */
@Component
public class RedisOAuthStateStore implements OAuthStateStore {

    private static final String KEY_PREFIX = "oauth:state:";
    private final StringRedisTemplate redis;

    public RedisOAuthStateStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * state를 Redis에 TTL과 함께 저장
     */
    @Override
    public void save(String state, OAuthProvider provider, long ttlSeconds) {
        redis.opsForValue().set(
                key(state, provider),
                "1",
                Duration.ofSeconds(ttlSeconds)
        );
    }

    /**
     * state 검증 후 삭제
     * - 존재하면 true 반환
     * - 존재하지 않으면 false
     */
    @Override
    public boolean consume(String state, OAuthProvider provider) {
        String key = key(state, provider);

        Boolean deleted = redis.delete(key); // 단일 명령으로 소비 판정
        return Boolean.TRUE.equals(deleted);
    }

    private String key(String state, OAuthProvider provider) {
        return KEY_PREFIX + provider.lower() + ":" + state;
    }
}