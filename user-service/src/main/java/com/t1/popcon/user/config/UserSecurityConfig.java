package com.t1.popcon.user.config;

import com.t1.popcon.common.auth.config.CommonSecurityConfig;
import com.t1.popcon.common.auth.filter.JwtFilter;
import com.t1.popcon.common.auth.handler.JwtAccessDeniedHandler;
import com.t1.popcon.common.auth.handler.JwtAuthenticationEntryPoint;
import com.t1.popcon.user.filter.InternalApiAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class UserSecurityConfig extends CommonSecurityConfig {

    private final InternalApiAuthFilter internalApiAuthFilter;

    public UserSecurityConfig(JwtFilter jwtFilter,
                              JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                              JwtAccessDeniedHandler jwtAccessDeniedHandler,
                              InternalApiAuthFilter internalApiAuthFilter) {
        super(jwtFilter, jwtAuthenticationEntryPoint, jwtAccessDeniedHandler);
        this.internalApiAuthFilter = internalApiAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        super.configureCommonSettings(http);

        http.addFilterBefore(internalApiAuthFilter, JwtFilter.class);

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/health",
                        "/v3/api-docs/**",
                        "/user/swagger-ui/**",
                        "/billing/**",
                        "/actuator/**"
                ).permitAll()
                .requestMatchers(
                        "/internal/users/**",
                        "/internal/billing/**",
                        "/users/internal/test-accounts/**"
                ).permitAll()
                .anyRequest().authenticated()
        );

        return http.build();
    }
}
