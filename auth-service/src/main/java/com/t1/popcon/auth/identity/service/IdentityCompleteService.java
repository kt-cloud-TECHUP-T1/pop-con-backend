package com.t1.popcon.auth.identity.service;

import com.t1.popcon.auth.client.user.UserServiceClient;
import com.t1.popcon.auth.client.user.dto.UserLookupResponse;
import com.t1.popcon.auth.common.RegisterPayload;
import com.t1.popcon.auth.common.RegisterTokenStore;
import com.t1.popcon.auth.identity.dto.IdentityCompleteRequest;
import com.t1.popcon.auth.identity.dto.IdentityCompleteResponse;
import com.t1.popcon.auth.token.domain.RefreshToken;
import com.t1.popcon.auth.token.domain.RefreshTokenRepository;
import com.t1.popcon.common.auth.config.JwtProperties;
import com.t1.popcon.common.auth.domain.TokenType;
import com.t1.popcon.common.auth.provider.TokenProvider;
import com.t1.popcon.common.encryption.EncryptionService;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.common.infrastructure.dto.PortOneIdentityVerificationResponse;
import com.t1.popcon.common.infrastructure.portone.PortOneClient;
import com.t1.popcon.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityCompleteService {

	private static final long REGISTER_TOKEN_EXTEND_SECONDS = 600L;

	private final PortOneClient portOneClient;
	private final RegisterTokenStore registerTokenStore;
	private final UserServiceClient userServiceClient;
	private final TokenProvider tokenProvider;
	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtProperties jwtProperties;
	private final EncryptionService encryptionService;

	@Transactional
	public IdentityCompleteResult complete(IdentityCompleteRequest request, String registerToken) {
		log.info("본인인증 완료 처리 시작 - identityVerificationId: {}, registerToken: {}",
				request.identityVerificationId(), registerToken);

		validateRegisterToken(registerToken);

		RegisterPayload payload = registerTokenStore.find(registerToken)
				.orElseThrow(() -> new CustomException(ErrorCode.REGISTER_TOKEN_EXPIRED));

		validateSocialPayload(payload);

		PortOneIdentityVerificationResponse verification =
				portOneClient.fetchIdentityVerification(request.identityVerificationId());

		if (!verification.isVerified() || verification.verifiedCustomer() == null) {
			throw new CustomException(ErrorCode.IDENTITY_VERIFICATION_FAILED);
		}

		PortOneIdentityVerificationResponse.VerifiedCustomer customer = verification.verifiedCustomer();

		validateVerifiedCustomer(customer);
		validateAge(customer.birthDate());

		String ciHash = encryptionService.generateHash(customer.ci());

		ApiResponse<UserLookupResponse> ciLookup = userServiceClient.findByCiHash(ciHash);
		if (ciLookup.getData() != null && ciLookup.getData().exists()) {
			Long userId = ciLookup.getData().userId();

			if (userId == null) {
				throw new CustomException(ErrorCode.IDENTITY_VERIFICATION_FAILED);
			}

			return linkAndLogin(registerToken, payload, ciHash, userId);
		}

		return mergeAndProceed(registerToken, ciHash, customer);
	}

	private void validateRegisterToken(String registerToken) {
		if (registerToken == null || registerToken.isBlank()) {
			throw new CustomException(ErrorCode.REGISTER_TOKEN_EXPIRED);
		}

		if (!registerTokenStore.exists(registerToken)) {
			throw new CustomException(ErrorCode.REGISTER_TOKEN_EXPIRED);
		}
	}

	private void validateSocialPayload(RegisterPayload payload) {
		if (payload.provider() == null
				|| payload.providerUserId() == null
				|| payload.providerUserId().isBlank()) {
			throw new CustomException(ErrorCode.SOCIAL_INFO_MISSING);
		}
	}

	private IdentityCompleteResult linkAndLogin(
			String registerToken,
			RegisterPayload payload,
			String ciHash,
			Long userId
	) {
		// 기존 계정에 소셜 계정 연결
		userServiceClient.linkSocialByCi(
				ciHash,
				payload.provider().name(),
				payload.providerUserId()
		);

		// 로그인용 토큰 발급
		String accessToken = tokenProvider.createToken(
				userId.toString(),
				jwtProperties.getAccessTokenExpiration(),
				TokenType.ACCESS
		);

		String refreshToken = tokenProvider.createToken(
				userId.toString(),
				jwtProperties.getRefreshTokenExpiration(),
				TokenType.REFRESH
		);

		long refreshTokenExpirationSeconds = jwtProperties.getRefreshTokenExpiration() / 1000;

		// refresh token은 해시 저장
		refreshTokenRepository.save(
				RefreshToken.builder()
						.userId(String.valueOf(userId))
						.token(tokenProvider.hashRefreshToken(refreshToken))
						.expiration(refreshTokenExpirationSeconds)
						.build()
		);

		// 가입 진행 토큰은 더 이상 필요 없으므로 삭제
		registerTokenStore.delete(registerToken);

		return IdentityCompleteResult.existingUser(
				IdentityCompleteResponse.ExistingUserComplete.of(userId, accessToken),
				refreshToken,
				refreshTokenExpirationSeconds
		);
	}

	private IdentityCompleteResult mergeAndProceed(
			String registerToken,
			String ciHash,
			PortOneIdentityVerificationResponse.VerifiedCustomer customer
	) {
		// 민감정보는 암호화 후 Redis payload에 병합
		String encryptedName = encryptionService.encrypt(customer.name());
		String encryptedGender = encryptionService.encrypt(customer.gender());
		String encryptedBirthDate = encryptionService.encrypt(customer.birthDate());
		String encryptedPhoneNumber = encryptionService.encrypt(customer.phoneNumber());
		String encryptedForeigner = encryptionService.encrypt(
				customer.isForeigner() == null ? null : String.valueOf(customer.isForeigner())
		);

		registerTokenStore.mergeIdentityVerification(
				registerToken,
				ciHash,
				encryptedName,
				encryptedGender,
				encryptedBirthDate,
				encryptedPhoneNumber,
				encryptedForeigner,
				REGISTER_TOKEN_EXTEND_SECONDS
		);

		return IdentityCompleteResult.newUser(
				IdentityCompleteResponse.NewUserComplete.terms()
		);
	}

	private void validateVerifiedCustomer(PortOneIdentityVerificationResponse.VerifiedCustomer customer) {
		if (customer.ci() == null || customer.ci().isBlank()) {
			throw new CustomException(ErrorCode.IDENTITY_VERIFICATION_FAILED);
		}
		if (customer.name() == null || customer.name().isBlank()) {
			throw new CustomException(ErrorCode.IDENTITY_VERIFICATION_FAILED);
		}
		if (customer.birthDate() == null || customer.birthDate().isBlank()) {
			throw new CustomException(ErrorCode.IDENTITY_VERIFICATION_FAILED);
		}
		if (customer.phoneNumber() == null || customer.phoneNumber().isBlank()) {
			throw new CustomException(ErrorCode.IDENTITY_VERIFICATION_FAILED);
		}
	}

	private void validateAge(String birthDateStr) {
		if (birthDateStr == null || birthDateStr.isBlank()) {
			throw new CustomException(ErrorCode.IDENTITY_VERIFICATION_FAILED);
		}

		try {
			LocalDate birthDate = LocalDate.parse(birthDateStr);
			LocalDate cutoffDate = LocalDate.now().minusYears(14);

			if (birthDate.isAfter(cutoffDate)) {
				throw new CustomException(ErrorCode.AGE_RESTRICTED);
			}
		} catch (DateTimeParseException e) {
			log.warn("생년월일 파싱 실패 - birthDate={}", birthDateStr, e);
			throw new CustomException(ErrorCode.IDENTITY_VERIFICATION_FAILED);
		}
	}

	public record IdentityCompleteResult(
			IdentityCompleteResponse response,
			String refreshToken,
			long refreshTokenExpiresIn
	) {
		public static IdentityCompleteResult existingUser(
				IdentityCompleteResponse.ExistingUserComplete response,
				String refreshToken,
				long refreshTokenExpiresIn
		) {
			return new IdentityCompleteResult(response, refreshToken, refreshTokenExpiresIn);
		}

		public static IdentityCompleteResult newUser(
				IdentityCompleteResponse.NewUserComplete response
		) {
			return new IdentityCompleteResult(response, null, 0L);
		}

		public boolean isExistingUser() {
			return response instanceof IdentityCompleteResponse.ExistingUserComplete;
		}
	}
}