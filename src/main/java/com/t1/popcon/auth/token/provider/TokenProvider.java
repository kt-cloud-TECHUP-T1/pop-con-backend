package com.t1.popcon.auth.token.provider;

import com.t1.popcon.auth.token.config.JwtProperties;
import com.t1.popcon.auth.token.domain.TokenType;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenProvider {

	private final JwtProperties jwtProperties;
	private SecretKey key;

	@PostConstruct
	protected void init() {
		this.key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
	}

	// 1. 토큰 생성 (Access/Refresh 공용)
	private static final String TOKEN_TYPE_CLAIM = "tokenType";
	public String createToken(String userId, long expirationTime, TokenType tokenType) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + expirationTime);
		return Jwts.builder()
			.subject(userId)
			.claim(TOKEN_TYPE_CLAIM, tokenType.name())
			.issuedAt(now)
			.expiration(expiry)
			.signWith(key)
			.compact();
	}
	// 2. 토큰에서 Authentication 객체 추출
	public Authentication getAuthentication(String token) {
		Claims claims = getClaims(token);
		String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
		if (!TokenType.ACCESS.name().equals(tokenType)) {
			throw new IllegalArgumentException("Access 토큰이 아닙니다.");
		}
		List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
		User principal = new User(claims.getSubject(), "", authorities);
		return new UsernamePasswordAuthenticationToken(principal, token, authorities);
	}

	// 3. 토큰 유효성 검사
	public boolean validateToken(String token) {
		Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
		return true;
	}

	private Claims getClaims(String token) {
		return Jwts.parser()
			.verifyWith(key)
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}
}