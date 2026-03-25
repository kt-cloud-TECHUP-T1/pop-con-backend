package com.t1.popcon.auth.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.t1.popcon.common.encryption.EncryptionService;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
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
	private final EncryptionService encryptionService;

    public RedisRegisterTokenStore(StringRedisTemplate redis, ObjectMapper objectMapper, EncryptionService encryptionService) {
        this.redis = redis;
        this.objectMapper = objectMapper;
	    this.encryptionService = encryptionService;
    }

    @Override
    public void save(String registerToken, RegisterPayload payload, long ttlSeconds) {
        if (ttlSeconds <= 0) ttlSeconds = DEFAULT_TTL_SECONDS;

        try {
            String json = toJson(payload);
            redis.opsForValue().set(key(registerToken), json, Duration.ofSeconds(ttlSeconds));
            log.debug("registerToken saved: {}", shortHash(registerToken));
        } catch (CustomException e) {
	        throw e;
        } catch (RedisConnectionFailureException e) {
	        log.error("registerToken Redis 저장 실패: registerTokenHash={}", shortHash(registerToken), e);
	        throw new CustomException(ErrorCode.ERROR_SYSTEM);
        } catch (Exception e) {
	        log.error("registerToken 저장 실패: registerTokenHash={}", shortHash(registerToken), e);
	        throw new CustomException(ErrorCode.ERROR_SYSTEM);
        }
    }

    @Override
    public boolean exists(String registerToken) {
	    try {
		    Boolean exists = redis.hasKey(key(registerToken));
		    return Boolean.TRUE.equals(exists);
	    } catch (RedisConnectionFailureException e) {
		    log.error("registerToken 존재 여부 조회 실패: registerTokenHash={}", shortHash(registerToken), e);
		    throw new CustomException(ErrorCode.ERROR_SYSTEM);
	    } catch (Exception e) {
		    log.error("registerToken 존재 여부 조회 중 오류: registerTokenHash={}", shortHash(registerToken), e);
		    throw new CustomException(ErrorCode.ERROR_SYSTEM);
	    }
    }

    @Override
    public Optional<RegisterPayload> find(String registerToken) {
	    try {
		    String json = redis.opsForValue().get(key(registerToken));
		    if (json == null || json.isBlank()) {
			    return Optional.empty();
		    }
		    return Optional.of(fromJson(json));
	    } catch (CustomException e) {
		    throw e;
	    } catch (RedisConnectionFailureException e) {
		    log.error("registerToken 조회 실패: registerTokenHash={}", shortHash(registerToken), e);
		    throw new CustomException(ErrorCode.ERROR_SYSTEM);
	    } catch (Exception e) {
		    log.error("registerToken 조회 중 오류: registerTokenHash={}", shortHash(registerToken), e);
		    throw new CustomException(ErrorCode.ERROR_SYSTEM);
	    }
    }

    @Override
    public Optional<RegisterPayload> consume(String registerToken) {
        try {
            String json = redis.opsForValue().getAndDelete(key(registerToken));
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            log.debug("registerToken consumed (GETDEL): {}", shortHash(registerToken));
            return Optional.of(fromJson(json));
        } catch (CustomException e) {
            throw e;
        } catch (RedisConnectionFailureException e) {
            log.error("registerToken 소모 실패: registerTokenHash={}", shortHash(registerToken), e);
            throw new CustomException(ErrorCode.ERROR_SYSTEM);
        } catch (Exception e) {
            log.error("registerToken 소모 중 오류: registerTokenHash={}", shortHash(registerToken), e);
            throw new CustomException(ErrorCode.ERROR_SYSTEM);
        }
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
                .orElseThrow(() -> new CustomException(ErrorCode.REGISTER_TOKEN_EXPIRED));

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
	    try {
		    redis.delete(key(registerToken));
		    log.debug("registerToken deleted: {}", shortHash(registerToken));
	    } catch (RedisConnectionFailureException e) {
		    log.error("registerToken 삭제 실패: registerTokenHash={}", shortHash(registerToken), e);
		    throw new CustomException(ErrorCode.ERROR_SYSTEM);
	    } catch (Exception e) {
		    log.error("registerToken 삭제 중 오류: registerTokenHash={}", shortHash(registerToken), e);
		    throw new CustomException(ErrorCode.ERROR_SYSTEM);
	    }
    }

    private String key(String registerToken) {
        return KEY_PREFIX + registerToken;
    }

    private String toJson(RegisterPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
	        log.error("RegisterPayload 직렬화 실패", e);
	        throw new CustomException(ErrorCode.ERROR_SYSTEM);
        }
    }

    private RegisterPayload fromJson(String json) {
        try {
            return objectMapper.readValue(json, RegisterPayload.class);
        } catch (JsonProcessingException e) {
	        log.error("RegisterPayload 역직렬화 실패", e);
	        throw new CustomException(ErrorCode.ERROR_SYSTEM);
        }
    }

	private String shortHash(String value) {
		if (value == null || value.isBlank()) {
			return "null";
		}
		String hashed = encryptionService.generateHash(value);
		return hashed.length() <= 8 ? hashed : hashed.substring(0, 8);
	}
}