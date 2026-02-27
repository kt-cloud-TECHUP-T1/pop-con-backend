package com.t1.popcon.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

@Getter
@Builder
@AllArgsConstructor
@RedisHash(value = "refreshToken")
public class RefreshToken {

	@Id
	private String userId; // 사용자 식별값 (Email 또는 고유 ID)

	@Indexed
	private String token; // 발급된 Refresh Token 값

	@TimeToLive
	private long expiration; // Redis 내 자동 삭제 시간 (초 단위)
}