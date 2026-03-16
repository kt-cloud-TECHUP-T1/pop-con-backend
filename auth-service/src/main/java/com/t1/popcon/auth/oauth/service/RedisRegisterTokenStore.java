package com.t1.popcon.auth.oauth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.t1.popcon.auth.oauth.dto.RegisterPayload;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class RedisRegisterTokenStore implements RegisterTokenStore {

    private static final String KEY_PREFIX = "oauth:register:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisRegisterTokenStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(String registerToken, RegisterPayload payload, long ttlSeconds) {
        if (ttlSeconds <= 0) ttlSeconds = 600;

        String json = toJson(payload);
        redis.opsForValue().set(key(registerToken), json, Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public Optional<RegisterPayload> find(String registerToken) {
        String json = redis.opsForValue().get(key(registerToken));
        if (json == null || json.isBlank()) return Optional.empty();

        return Optional.of(fromJson(json));
    }

    @Override
    public void mergeCiHash(String registerToken, String ciHash, long ttlSecondsToExtend) {
        RegisterPayload payload = find(registerToken)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));

        RegisterPayload updated = payload.withCiHash(ciHash);

        if (ttlSecondsToExtend <= 0) ttlSecondsToExtend = 600;
        save(registerToken, updated, ttlSecondsToExtend);
    }

    @Override
    public void delete(String registerToken) {
        redis.delete(key(registerToken));
    }

    private String key(String registerToken) {
        return KEY_PREFIX + registerToken;
    }

    private String toJson(RegisterPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize RegisterPayload", e);
        }
    }

    private RegisterPayload fromJson(String json) {
        try {
            return objectMapper.readValue(json, RegisterPayload.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize RegisterPayload", e);
        }
    }
}