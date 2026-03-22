package com.t1.popcon.auth.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
public class RedisRegisterTokenStore implements RegisterTokenStore {

    private static final String KEY_PREFIX = "oauth:register:";
    private static final long DEFAULT_TTL_SECONDS = 600L;

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisRegisterTokenStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(String registerToken, RegisterPayload payload, long ttlSeconds) {
        if (ttlSeconds <= 0) ttlSeconds = DEFAULT_TTL_SECONDS;

        try {
            String json = toJson(payload);
            redis.opsForValue().set(key(registerToken), json, Duration.ofSeconds(ttlSeconds));
            log.debug("registerToken saved: {}", registerToken);
        } catch (Exception e) {
            log.error("registerToken 저장 실패: registerToken={}", registerToken, e);
            throw e;
        }
    }

    @Override
    public boolean exists(String registerToken) {
        Boolean exists = redis.hasKey(key(registerToken));
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public Optional<RegisterPayload> find(String registerToken) {
        String json = redis.opsForValue().get(key(registerToken));
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(fromJson(json));
    }

    @Override
    public void mergeIdentityVerification(
            String registerToken,
            String ciHash,
            String encryptedName,
            String encryptedGender,
            String encryptedBirthDate,
            String encryptedPhoneNumber,
            String encryptedNationality,
            long ttlSecondsToExtend
    ) {
        RegisterPayload payload = find(registerToken)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));

        RegisterPayload updated = payload.withIdentityVerification(
                ciHash,
                encryptedName,
                encryptedGender,
                encryptedBirthDate,
                encryptedPhoneNumber,
                encryptedNationality
        );

        if (ttlSecondsToExtend <= 0) ttlSecondsToExtend = DEFAULT_TTL_SECONDS;
        save(registerToken, updated, ttlSecondsToExtend);
    }

    @Override
    public void delete(String registerToken) {
        redis.delete(key(registerToken));
        log.debug("registerToken deleted: {}", registerToken);
    }

    private String key(String registerToken) {
        return KEY_PREFIX + registerToken;
    }

    private String toJson(RegisterPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("RegisterPayload 직렬화 실패", e);
        }
    }

    private RegisterPayload fromJson(String json) {
        try {
            return objectMapper.readValue(json, RegisterPayload.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("RegisterPayload 역직렬화 실패", e);
        }
    }
}