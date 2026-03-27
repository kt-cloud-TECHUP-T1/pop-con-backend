package com.t1.popcon.common.auth.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.t1.popcon.common.auth.domain.AuthUser;

@Configuration
@EnableJpaAuditing
public class JpaConfig {
	@Bean
	public AuditorAware<Long> auditorAware() {
		return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
			.filter(Authentication::isAuthenticated)
			.map(Authentication::getPrincipal)
			.filter(AuthUser.class::isInstance)
			.map(AuthUser.class::cast)
			.map(AuthUser::id);
	}
}
