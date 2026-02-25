package com.t1.popcon.support.config;

import static org.mockito.Mockito.*;

import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@TestConfiguration
@Profile("test")
public class TestRedisConfig {
	@Bean
	@Primary
	public RedissonClient redissonClient() {
		return mock(RedissonClient.class);
	}

	@Bean
	@Primary
	public StringRedisTemplate stringRedisTemplate() {
		StringRedisTemplate template = mock(StringRedisTemplate.class);

		when(template.getConnectionFactory()).thenReturn(mock(RedisConnectionFactory.class));

		return template;
	}
}
