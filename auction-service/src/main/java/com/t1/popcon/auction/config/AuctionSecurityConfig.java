package com.t1.popcon.auction.config;

import com.t1.popcon.common.auth.config.CommonSecurityConfig;
import com.t1.popcon.common.auth.filter.JwtFilter;
import com.t1.popcon.common.auth.handler.JwtAccessDeniedHandler;
import com.t1.popcon.common.auth.handler.JwtAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class AuctionSecurityConfig extends CommonSecurityConfig {

    private final InternalApiAuthFilter internalApiAuthFilter;

    public AuctionSecurityConfig(JwtFilter jwtFilter,
      JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
      JwtAccessDeniedHandler jwtAccessDeniedHandler,
      InternalApiAuthFilter internalApiAuthFilter) {
        super(jwtFilter, jwtAuthenticationEntryPoint, jwtAccessDeniedHandler);
        this.internalApiAuthFilter = internalApiAuthFilter;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain auctionSecurityFilterChain(HttpSecurity http) throws Exception {
        super.configureCommonSettings(http);

        http
          .securityMatcher("/auctions/**", "/admin/auctions/**", "/internal/**")
          .authorizeHttpRequests(auth -> auth
            .requestMatchers("/internal/**").permitAll()
            .requestMatchers("/auctions/**").permitAll()
            .anyRequest().authenticated()
          )
          .addFilterBefore(internalApiAuthFilter, JwtFilter.class);

        return http.build();
    }
}