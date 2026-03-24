package com.t1.popcon.auth.signup.service;

import com.t1.popcon.auth.oauth.dto.RegisterPayload;
import com.t1.popcon.auth.oauth.service.RegisterTokenStore;
import com.t1.popcon.auth.signup.client.SignUpUserServiceClient;
import com.t1.popcon.auth.signup.client.dto.UserCreateRequest;
import com.t1.popcon.auth.signup.client.dto.UserCreateResponse;
import com.t1.popcon.auth.signup.dto.SignUpRequest;
import com.t1.popcon.auth.signup.dto.SignUpResponse;
import com.t1.popcon.auth.token.domain.RefreshToken;
import com.t1.popcon.auth.token.domain.RefreshTokenRepository;
import com.t1.popcon.common.auth.config.JwtProperties;
import com.t1.popcon.common.auth.domain.TokenType;
import com.t1.popcon.common.auth.provider.TokenProvider;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SignUpService {

	private final RegisterTokenStore registerTokenStore;
	private final SignUpUserServiceClient userServiceClient;
	private final TokenProvider tokenProvider;
	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtProperties jwtProperties;

	@Transactional
	public SignupResult signup(String registerToken, SignUpRequest.Signup request) {
		// 1. registerToken으로 Redis에서 임시 가입 정보 조회
		RegisterPayload payload = registerTokenStore.find(registerToken)
			.orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));

		// 2. user-service에 회원 저장 요청
		UserCreateRequest createRequest = UserCreateRequest.of(payload, request.agreements());
		ApiResponse<UserCreateResponse> response = userServiceClient.createUser(createRequest);
		UserCreateResponse savedUser = response.getData();

		if (savedUser == null) {
			throw new CustomException(ErrorCode.ERROR_SYSTEM, "사용자 생성에 실패했습니다.");
		}

		// 3. 토큰 발급
		String userId = String.valueOf(savedUser.id());
		String accessToken = tokenProvider.createToken(userId, jwtProperties.getAccessTokenExpiration(), TokenType.ACCESS);
		String refreshToken = tokenProvider.createToken(userId, jwtProperties.getRefreshTokenExpiration(), TokenType.REFRESH);

		// 4. Redis에 Refresh Token 저장
		refreshTokenRepository.save(RefreshToken.builder()
			.userId(userId)
			.token(tokenProvider.hashRefreshToken(refreshToken))
			.expiration(jwtProperties.getRefreshTokenExpiration() / 1000)
			.build());

		// 5. 사용 완료된 registerToken 삭제
		registerTokenStore.delete(registerToken);

		return new SignupResult(savedUser, accessToken, refreshToken);
	}

	public record SignupResult(
		UserCreateResponse user,
		String accessToken,
		String refreshToken
	) {
		public SignUpResponse.Signup toResponse() {
			return new SignUpResponse.Signup(
				user.id(),
				user.name(),
				accessToken,
				LocalDateTime.now()
			);
		}
	}
}
