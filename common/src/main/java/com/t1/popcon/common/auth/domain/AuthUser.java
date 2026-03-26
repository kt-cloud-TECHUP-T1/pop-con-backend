package com.t1.popcon.common.auth.domain;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public record AuthUser(
	Long id,
	Collection<? extends GrantedAuthority> authorities
) {
}
