package com.t1.popcon.auction.bid.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Collections;

@Slf4j
@Repository
@RequiredArgsConstructor
public class BidRedisRepository {

	private final RedisTemplate<String, String> redisTemplate;
	private static final String STOCK_KEY_PREFIX = "auction:option:%d:stock";

	public Integer getStock(Long optionId) {
		String key = String.format(STOCK_KEY_PREFIX, optionId);
		String stock = redisTemplate.opsForValue().get(key);
		return stock != null ? Integer.parseInt(stock) : 0;
	}

	public void setStock(Long optionId, Integer stock) {
		String key = String.format(STOCK_KEY_PREFIX, optionId);
		redisTemplate.opsForValue().set(key, String.valueOf(stock));
	}

	// 결제 실패 시 재고 복구
	public void incrementStock(Long optionId) {
		String key = String.format(STOCK_KEY_PREFIX, optionId);
		redisTemplate.opsForValue().increment(key);
	}

	// Lua Script를 이용한 원자적 재고 차감
	private static final DefaultRedisScript<Long> DECREMENT_SCRIPT;
	static {
		DECREMENT_SCRIPT = new DefaultRedisScript<>();
		DECREMENT_SCRIPT.setLocation(new ClassPathResource("scripts/stock_decrement.lua"));
		DECREMENT_SCRIPT.setResultType(Long.class);
	}

	public Long decrementStock(Long optionId) {
		String key = String.format(STOCK_KEY_PREFIX, optionId);
		return redisTemplate.execute(DECREMENT_SCRIPT, Collections.singletonList(key), "1");
	}
}