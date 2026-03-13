package com.t1.popcon.config;

import com.t1.popcon.common.auth.config.CommonSecurityConfig;
import com.t1.popcon.common.auth.filter.JwtFilter;
import com.t1.popcon.common.auth.handler.JwtAccessDeniedHandler;
import com.t1.popcon.common.auth.handler.JwtAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class PopupSecurityConfig extends CommonSecurityConfig {

	public PopupSecurityConfig(JwtFilter jwtFilter,
		JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
		JwtAccessDeniedHandler jwtAccessDeniedHandler) {
		super(jwtFilter, jwtAuthenticationEntryPoint, jwtAccessDeniedHandler);
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		super.configureCommonSettings(http);

		http.authorizeHttpRequests(auth -> auth
			.requestMatchers(
				"/health",
				"/v3/api-docs/**",
				"/swagger-ui/**",
				"/health",           // 헬스체크
				"/actuator/**"
			).permitAll()
			.anyRequest().authenticated()
		);

		return http.build();
	}
}