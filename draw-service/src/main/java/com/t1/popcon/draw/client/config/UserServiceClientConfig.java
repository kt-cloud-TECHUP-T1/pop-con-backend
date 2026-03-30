package com.t1.popcon.draw.client.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.RequestInterceptor;

@Configuration
public class UserServiceClientConfig {

	@Value("${internal.api-secret}")
	private String internalSecret;

	@Bean
	public RequestInterceptor internalSecretInterceptor() {
		return template -> template.header("X-Internal-Secret", internalSecret);
	}
}