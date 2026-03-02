package com.t1.popcon.auth.signup.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.t1.popcon.auth.token.config.JwtProperties;
import com.t1.popcon.auth.token.domain.TokenType;
import com.t1.popcon.auth.signup.dto.AuthRequest;
import com.t1.popcon.auth.signup.dto.AuthResponse;
import com.t1.popcon.auth.token.domain.RefreshToken;
import com.t1.popcon.auth.token.provider.TokenProvider;
import com.t1.popcon.auth.token.domain.RefreshTokenRepository;
import com.t1.popcon.user.domain.Gender;
import com.t1.popcon.user.domain.User;
import com.t1.popcon.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final TokenProvider tokenProvider;
	private final RefreshTokenRepository refreshTokenRepository;
	private final UserRepository userRepository;
	private final JwtProperties jwtProperties;

	@Transactional
	public AuthResponse.Signup signup(AuthRequest.Signup request) {
		// TODO: 1. registerToken으로 Redis에서 본인인증 정보를 가져오는 로직이 들어갈 자리입니다.
		// 지금은 테스트를 위해 가짜 정보를 변수에 담아 유저를 생성합니다.
		String nameFromAuth = "홍길동";
		String ciHashFromAuth = "test_ci_1234" + System.currentTimeMillis();

		// 2. 신규 유저 생성 및 DB 저장
		User newUser = User.createUserWithKakao(
			ciHashFromAuth,
			LocalDateTime.now(),
			nameFromAuth,
			"01012345678",
			LocalDate.of(1995, 1, 1),
			Gender.MALE,
			"kakao_dummy_id"
		);

		User savedUser = userRepository.save(newUser);

		// 3. 생성된 유저의 ID로 JWT 발급
		String accessToken = tokenProvider.createToken(String.valueOf(savedUser.getId()), jwtProperties.getAccessTokenExpiration(), TokenType.ACCESS);
		String refreshTokenString = tokenProvider.createToken(String.valueOf(savedUser.getId()), jwtProperties.getRefreshTokenExpiration(), TokenType.REFRESH);

		// 4. Redis에 Refresh Token 저장
		refreshTokenRepository.save(RefreshToken.builder()
			.userId(String.valueOf(savedUser.getId()))
			.token(refreshTokenString)
			.expiration(jwtProperties.getRefreshTokenExpiration() / 1000)
			.build());

		return new AuthResponse.Signup(
			savedUser.getId(),
			savedUser.getName(),
			accessToken,
			refreshTokenString,
			savedUser.getCreatedAt()
		);
	}
}