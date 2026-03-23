package com.t1.popcon.auth.identity.controller;

import com.t1.popcon.auth.identity.dto.IdentityCompleteRequest;
import com.t1.popcon.auth.identity.dto.IdentityCompleteResponse;
import com.t1.popcon.auth.identity.service.IdentityCompleteService;
import com.t1.popcon.auth.common.config.FrontendProperties;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth/identity")
@RequiredArgsConstructor
public class IdentityCompleteController {

	private static final String REGISTER_TOKEN_COOKIE = "register_token";
	private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

	private final IdentityCompleteService identityCompleteService;
	private final FrontendProperties frontendProperties;

	@PostMapping("/complete")
	public ResponseEntity<ApiResponse<IdentityCompleteResponse>> complete(
			@Valid @RequestBody IdentityCompleteRequest.Complete request,
			@RequestHeader(value = "X-Device-Id", required = false) String deviceId,
			@CookieValue(name = REGISTER_TOKEN_COOKIE, required = false) String registerToken
	) {
		// 가입 진행용 쿠키는 필수
		if (registerToken == null || registerToken.isBlank()) {
			log.warn("register_token 쿠키 누락");
			throw new CustomException(ErrorCode.REGISTER_TOKEN_EXPIRED);
		}

		IdentityCompleteService.IdentityCompleteResult result =
				identityCompleteService.complete(request, registerToken);

		IdentityCompleteResponse response = result.response();

		// 기존 회원이면 refresh_token 발급 + register_token 삭제
		if (result.isExistingUser()) {
			return buildExistingUserResponse(
					(IdentityCompleteResponse.ExistingUserComplete) response,
					result.refreshToken(),
					result.refreshTokenExpiresIn()
			);
		}

		// 신규 회원이면 register_token 유지 후 약관 단계로 이동
		return buildNewUserResponse(response);
	}

	/**
	 * 기존 회원 응답
	 * - refresh_token 발급
	 * - register_token 삭제
	 */
	private ResponseEntity<ApiResponse<IdentityCompleteResponse>> buildExistingUserResponse(
			IdentityCompleteResponse.ExistingUserComplete response,
			String refreshToken,
			long refreshTokenExpiresIn
	) {
		ResponseCookie refreshCookie = buildCookie(
				REFRESH_TOKEN_COOKIE,
				refreshToken,
				refreshTokenExpiresIn
		);

		ResponseCookie registerCookie = buildCookie(
				REGISTER_TOKEN_COOKIE,
				"",
				0
		);

		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
				.header(HttpHeaders.SET_COOKIE, registerCookie.toString())
				.body(ApiResponse.ok("계정 연결 및 로그인이 완료되었습니다.", response));
	}

	/**
	 * 신규 회원 응답
	 * - register_token 유지
	 */
	private ResponseEntity<ApiResponse<IdentityCompleteResponse>> buildNewUserResponse(
			IdentityCompleteResponse response
	) {
		return ResponseEntity.ok(
				ApiResponse.ok("본인인증이 완료되었습니다. 약관에 동의해주세요.", response)
		);
	}

	private ResponseCookie buildCookie(String name, String value, long maxAgeSeconds) {
		ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
				.httpOnly(true)
				.secure(frontendProperties.isCookieSecure())
				.path("/")
				.maxAge(maxAgeSeconds)
				.sameSite(resolveSameSite());

		String domain = frontendProperties.cookieDomain();
		if (domain != null && !domain.isBlank()) {
			builder.domain(domain);
		}

		return builder.build();
	}

	private String resolveSameSite() {
		String sameSite = frontendProperties.cookieSameSite();
		return (sameSite == null || sameSite.isBlank()) ? "Lax" : sameSite;
	}
}