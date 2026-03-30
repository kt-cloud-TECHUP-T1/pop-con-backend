package com.t1.popcon.auction.bid.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class BidRedisRepository {

	private static final String AVAILABLE_STOCK_KEY_PREFIX = "auction:option:%d:stock";
	private static final String PENDING_RESTOCK_KEY_PREFIX = "auction:option:%d:pending-restock";
	private static final String SOLD_OUT_PRICE_KEY_PREFIX = "auction:%d:sold-out-price";
	private static final String RESTOCK_ANCHOR_AT_KEY_PREFIX = "auction:%d:restock-anchor-at";

	private final RedisTemplate<String, String> redisTemplate;

	public Integer getAvailableStock(Long optionId) {
		String stock = redisTemplate.opsForValue().get(getAvailableStockKey(optionId));
		return stock != null ? Integer.parseInt(stock) : null;
	}

	public int getPendingRestock(Long optionId) {
		String stock = redisTemplate.opsForValue().get(getPendingRestockKey(optionId));
		return stock != null ? Integer.parseInt(stock) : 0;
	}

	public void setAvailableStock(Long optionId, Integer stock) {
		redisTemplate.opsForValue().set(getAvailableStockKey(optionId), String.valueOf(stock));
	}

	public void addPendingRestock(Long optionId, long quantity) {
		redisTemplate.opsForValue().increment(getPendingRestockKey(optionId), quantity);
	}

	public void incrementAvailableStock(Long optionId, long quantity) {
		redisTemplate.opsForValue().increment(getAvailableStockKey(optionId), quantity);
	}

	public Integer getSoldOutPrice(Long auctionId) {
		String value = redisTemplate.opsForValue().get(getSoldOutPriceKey(auctionId));
		return value != null ? Integer.parseInt(value) : null;
	}

	public void setSoldOutPrice(Long auctionId, Integer soldOutPrice) {
		redisTemplate.opsForValue().set(getSoldOutPriceKey(auctionId), String.valueOf(soldOutPrice));
	}

	public LocalDateTime getRestockAnchorAt(Long auctionId) {
		String value = redisTemplate.opsForValue().get(getRestockAnchorAtKey(auctionId));
		return value != null ? LocalDateTime.parse(value) : null;
	}

	public void setRestockAnchorAt(Long auctionId, LocalDateTime restockAnchorAt) {
		redisTemplate.opsForValue().set(getRestockAnchorAtKey(auctionId), restockAnchorAt.toString());
	}

	public void clearRestockAnchorAt(Long auctionId) {
		redisTemplate.delete(getRestockAnchorAtKey(auctionId));
	}

	private static final DefaultRedisScript<Long> DECREMENT_SCRIPT;
	static {
		DECREMENT_SCRIPT = new DefaultRedisScript<>();
		DECREMENT_SCRIPT.setLocation(new ClassPathResource("scripts/stock_decrement.lua"));
		DECREMENT_SCRIPT.setResultType(Long.class);
	}

	public Long decrementStock(Long optionId) {
		return redisTemplate.execute(DECREMENT_SCRIPT, Collections.singletonList(getAvailableStockKey(optionId)), "1");
	}

	private static final DefaultRedisScript<Long> RELEASE_PENDING_RESTOCK_SCRIPT;
	static {
		RELEASE_PENDING_RESTOCK_SCRIPT = new DefaultRedisScript<>();
		RELEASE_PENDING_RESTOCK_SCRIPT.setScriptText("""
			local pending = redis.call('GET', KEYS[2])
			if not pending or tonumber(pending) <= 0 then
			    return 0
			end

			local released = tonumber(pending)
			redis.call('INCRBY', KEYS[1], released)
			redis.call('DEL', KEYS[2])
			return released
			""");
		RELEASE_PENDING_RESTOCK_SCRIPT.setResultType(Long.class);
	}

	public long releasePendingRestock(Long optionId) {
		Long released = redisTemplate.execute(
			RELEASE_PENDING_RESTOCK_SCRIPT,
			List.of(getAvailableStockKey(optionId), getPendingRestockKey(optionId))
		);
		return released != null ? released : 0L;
	}

	private String getAvailableStockKey(Long optionId) {
		return String.format(AVAILABLE_STOCK_KEY_PREFIX, optionId);
	}

	private String getPendingRestockKey(Long optionId) {
		return String.format(PENDING_RESTOCK_KEY_PREFIX, optionId);
	}

	private String getSoldOutPriceKey(Long auctionId) {
		return String.format(SOLD_OUT_PRICE_KEY_PREFIX, auctionId);
	}

	private String getRestockAnchorAtKey(Long auctionId) {
		return String.format(RESTOCK_ANCHOR_AT_KEY_PREFIX, auctionId);
	}
}
