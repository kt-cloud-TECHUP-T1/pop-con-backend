package com.t1.popcon.draw.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.t1.popcon.common.auth.config.CommonSecurityConfig;
import com.t1.popcon.common.auth.filter.JwtFilter;
import com.t1.popcon.common.auth.handler.JwtAccessDeniedHandler;
import com.t1.popcon.common.auth.handler.JwtAuthenticationEntryPoint;
import com.t1.popcon.common.queue.QuizPassedTokenFilter;
import com.t1.popcon.common.queue.QuizPassedTokenValidator;
import com.t1.popcon.draw.filter.InternalApiAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class DrawSecurityConfig extends CommonSecurityConfig {

    private final InternalApiAuthFilter internalApiAuthFilter;
    private final QuizPassedTokenValidator quizPassedTokenValidator;
    private final ObjectMapper objectMapper;

    public DrawSecurityConfig(
            JwtFilter jwtFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            JwtAccessDeniedHandler jwtAccessDeniedHandler,
            InternalApiAuthFilter internalApiAuthFilter,
            QuizPassedTokenValidator quizPassedTokenValidator,
            ObjectMapper objectMapper
    ) {
        super(jwtFilter, jwtAuthenticationEntryPoint, jwtAccessDeniedHandler);
        this.internalApiAuthFilter = internalApiAuthFilter;
        this.quizPassedTokenValidator = quizPassedTokenValidator;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        super.configureCommonSettings(http);

        http.addFilterBefore(internalApiAuthFilter, JwtFilter.class);

        // 퀴즈 통과 토큰 검증 필터 추가
        // - 상세 조회(GET /draws/{id}), 공용 API, 내부 호출은 제외
        QuizPassedTokenFilter quizFilter = new QuizPassedTokenFilter(quizPassedTokenValidator, objectMapper) {
            @Override
            protected boolean shouldNotFilter(jakarta.servlet.http.HttpServletRequest request) {
                String path = request.getRequestURI();
                String method = request.getMethod();

                // 1. 상세 조회 제외
                if (method.equals("GET") && path.matches("^/draws/\\d+$")) return true;
                // 1-1. 응모 완료 후 결과 확인은 퀴즈 토큰 정리 이후에 호출된다.
                if (method.equals("POST") && path.matches("^/draws/entries/\\d+/confirm-result$")) return true;
                // 2. 공용 및 내부 API 제외
                if (path.startsWith("/internal/") || path.equals("/health") || path.startsWith("/actuator/")) return true;
                // 3. API 문서 제외
                return path.startsWith("/v3/api-docs") || path.startsWith("/draw/swagger-ui");
            }
        };
        http.addFilterAfter(quizFilter, JwtFilter.class);

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/health",
                        "/v3/api-docs/**",
                        "/draw/swagger-ui/**",
                        "/actuator/**"
                ).permitAll()
                .requestMatchers("/internal/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/draws/{drawId}").authenticated()
                .requestMatchers("/draws/**").authenticated()
                .anyRequest().authenticated()
        );

        return http.build();
    }
}
