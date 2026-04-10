package com.t1.popcon.queue.config;

import com.t1.popcon.common.auth.config.CommonSecurityConfig;
import com.t1.popcon.common.auth.filter.JwtFilter;
import com.t1.popcon.common.auth.handler.JwtAccessDeniedHandler;
import com.t1.popcon.common.auth.handler.JwtAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * queue-service 보안 설정
 * - 현재: 개발 편의상 전체 permitAll (TODO: 운영 전 인증 복구)
 *   - POST /queues/** : JWT 인증 필수 (userId 식별)
 *   - GET /queues/status, DELETE /queues : queueToken 기반 인증 (서비스 레이어에서 검증)
 */
@Configuration
@EnableWebSecurity
public class QueueSecurityConfig extends CommonSecurityConfig {

    public QueueSecurityConfig(
            JwtFilter jwtFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            JwtAccessDeniedHandler jwtAccessDeniedHandler
    ) {
        super(jwtFilter, jwtAuthenticationEntryPoint, jwtAccessDeniedHandler);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        super.configureCommonSettings(http);

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/health", "/actuator/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/queues/status").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/queues").authenticated()
                .requestMatchers(HttpMethod.POST, "/queues/**").authenticated()
                .anyRequest().authenticated()
        );

        return http.build();
    }
}
