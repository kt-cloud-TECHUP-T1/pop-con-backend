package com.t1.popcon.auction.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.t1.popcon.common.auth.config.CommonSecurityConfig;
import com.t1.popcon.common.auth.filter.JwtFilter;
import com.t1.popcon.common.auth.handler.JwtAccessDeniedHandler;
import com.t1.popcon.common.auth.handler.JwtAuthenticationEntryPoint;
import com.t1.popcon.common.queue.QuizPassedTokenFilter;
import com.t1.popcon.common.queue.QuizPassedTokenValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class AuctionSecurityConfig extends CommonSecurityConfig {

    private final InternalApiAuthFilter internalApiAuthFilter;
    private final QuizPassedTokenValidator quizPassedTokenValidator;
    private final ObjectMapper objectMapper;

    public AuctionSecurityConfig(JwtFilter jwtFilter,
      JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
      JwtAccessDeniedHandler jwtAccessDeniedHandler,
      InternalApiAuthFilter internalApiAuthFilter,
      QuizPassedTokenValidator quizPassedTokenValidator,
      ObjectMapper objectMapper) {
        super(jwtFilter, jwtAuthenticationEntryPoint, jwtAccessDeniedHandler);
        this.internalApiAuthFilter = internalApiAuthFilter;
        this.quizPassedTokenValidator = quizPassedTokenValidator;
        this.objectMapper = objectMapper;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain auctionSecurityFilterChain(HttpSecurity http) throws Exception {
        super.configureCommonSettings(http);

        // 퀴즈 통과 토큰 검증 필터 추가
        // - 어드민, 내부 호출, 상세 조회(GET)는 퀴즈 검증 제외
        QuizPassedTokenFilter quizFilter = new QuizPassedTokenFilter(quizPassedTokenValidator, objectMapper) {
            @Override
            protected boolean shouldNotFilter(jakarta.servlet.http.HttpServletRequest request) {
                String path = request.getRequestURI();
                String method = request.getMethod();

                // 1. 상세 조회 제외
                if (method.equals("GET") && path.matches("^/auctions/\\d+$")) return true;
                // 2. 어드민 및 내부 API 제외
                if (path.startsWith("/admin/") || path.startsWith("/internal/")) return true;
                // 3. 헬스체크 등 공용 API 제외
                return path.equals("/health") || path.startsWith("/actuator/");
            }
        };

        http
          .securityMatcher("/auctions/**", "/bids/**", "/admin/auctions/**", "/internal/**")
          .authorizeHttpRequests(auth -> auth
            .requestMatchers("/internal/**").permitAll()
            .requestMatchers(HttpMethod.GET,"/auctions/{auctionId}").permitAll()
            .requestMatchers("/auctions/**", "/bids/**").authenticated() // 나머지는 퀴즈 토큰 필요
            .anyRequest().authenticated()
          )
          .addFilterBefore(internalApiAuthFilter, JwtFilter.class)
          .addFilterAfter(quizFilter, JwtFilter.class); // JWT 필터 이후에 실행

        return http.build();
    }
}