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
        // - 상세 조회(GET /draws/{id})는 제외하고, 날짜/옵션 조회 및 응모 시에만 작동하도록 설정
        QuizPassedTokenFilter quizFilter = new QuizPassedTokenFilter(quizPassedTokenValidator, objectMapper) {
            @Override
            protected boolean shouldNotFilter(jakarta.servlet.http.HttpServletRequest request) {
                String path = request.getRequestURI();
                String method = request.getMethod();
                return method.equals("GET") && path.matches("^/draws/\\d+$");
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
