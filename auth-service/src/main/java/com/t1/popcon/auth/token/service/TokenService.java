package com.t1.popcon.auth.token.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.t1.popcon.common.auth.config.JwtProperties;
import com.t1.popcon.auth.token.domain.RefreshToken;
import com.t1.popcon.auth.token.domain.RefreshTokenRepository;
import com.t1.popcon.common.auth.domain.TokenType;
import com.t1.popcon.auth.token.dto.TokenRefreshRequest;
import com.t1.popcon.auth.token.dto.TokenRefreshResponse;
import com.t1.popcon.common.auth.provider.TokenProvider;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenService {

	private final TokenProvider tokenProvider;
	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtProperties jwtProperties;

	@Transactional
	public TokenRefreshResponse reissueToken(TokenRefreshRequest request) {
		final String refreshToken = request.refreshToken();

		// 1. Refresh Token 유효성 검증 (서명, 만료)
		if (!tokenProvider.validateToken(refreshToken)) {
			throw new CustomException(ErrorCode.INVALID_TOKEN);
		}

		// 2. Refresh Token 타입 검증
		if (!TokenType.REFRESH.name().equals(tokenProvider.getTokenType(refreshToken))) {
			throw new CustomException(ErrorCode.INVALID_TOKEN);
		}

		// 3. 토큰에서 유저 ID 추출
		String userId = tokenProvider.getSubject(refreshToken);

		// 4. Redis에 저장된 Refresh Token 조회 및 검증
		RefreshToken storedToken = refreshTokenRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.TOKEN_EXPIRED)); // 로그아웃 되었거나 만료됨

		String hashedRequestToken = tokenProvider.hashRefreshToken(refreshToken);

		if (!storedToken.getToken().equals(hashedRequestToken)) {
			// 탈취된 토큰 사용 시도 감지. 보안을 위해 해당 유저의 토큰을 무효화.
			refreshTokenRepository.delete(storedToken);
			throw new CustomException(ErrorCode.INVALID_TOKEN);
		}

		// 5. 새로운 토큰 쌍 생성 (RTR)
		String newAccessToken = tokenProvider.createToken(userId, jwtProperties.getAccessTokenExpiration(), TokenType.ACCESS);
		String newRefreshToken = tokenProvider.createToken(userId, jwtProperties.getRefreshTokenExpiration(), TokenType.REFRESH);

		// 6. Redis 업데이트
		long refreshTokenExpirationSeconds = jwtProperties.getRefreshTokenExpiration() / 1000;
		refreshTokenRepository.save(RefreshToken.builder()
			.userId(userId)
			.token(tokenProvider.hashRefreshToken(newRefreshToken))
			.expiration(refreshTokenExpirationSeconds)
			.build());

		// 7. 새로운 토큰 반환
		return TokenRefreshResponse.builder()
			.accessToken(newAccessToken)
			.refreshToken(newRefreshToken)
			.expiresIn(jwtProperties.getRefreshTokenExpiration() / 1000)
			.build();
	}
}