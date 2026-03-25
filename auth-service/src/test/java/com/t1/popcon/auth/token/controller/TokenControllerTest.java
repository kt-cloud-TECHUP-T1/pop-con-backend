package com.t1.popcon.auth.token.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.t1.popcon.auth.token.dto.TokenRefreshResponse;
import com.t1.popcon.auth.token.service.TokenService;
import com.t1.popcon.auth.token.service.TokenService.TokenReissueResult;
import com.t1.popcon.auth.token.util.CookieProvider;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.support.AbstractRestDocsTest;
import com.t1.popcon.support.RestDocsFactory;

import jakarta.servlet.http.Cookie;

@ActiveProfiles("test")
class TokenControllerTest extends AbstractRestDocsTest {

	private static final String DEFAULT_URL = "/auth/token/refresh";

	@Autowired
	private RestDocsFactory restDocsFactory;

	@MockitoBean
	private TokenService tokenService;

	@MockitoBean
	private CookieProvider cookieProvider;

	@BeforeEach
	void setUpMocks() {
		ResponseCookie emptyCookie = ResponseCookie.from(CookieProvider.REFRESH_TOKEN_COOKIE, "")
			.path("/")
			.maxAge(0)
			.build();
		given(cookieProvider.removeRefreshTokenCookie()).willReturn(emptyCookie);
	}

	@Nested
	class 토큰_재발급_API {

		private static final String SUMMARY = "토큰 재발급";
		private static final String DESCRIPTION = """
			Refresh Token 쿠키를 사용하여 새로운 Access Token(바디)과 Refresh Token(쿠키)을 발급받습니다.
			
			[에러 케이스]
			- 400: refresh_token 쿠키 누락
			- 401 (A002): 인증 정보가 유효하지 않음 (refreshToken 위조/서명 실패)
			- 401 (A003): 인증 만료/세션 만료 (refreshToken 만료 또는 폐기됨)
			- 500 (S001): 서버 오류
			""";

		@Test
		void 성공() throws Exception {
			// given
			final String requestRefreshToken = "valid.refresh.token";
			final String newAccessToken = "new.access.token";
			final String newRefreshToken = "new.refresh.token";
			final long expiresIn = 3600L;

			TokenReissueResult serviceResult = new TokenReissueResult(newAccessToken, newRefreshToken, expiresIn);
			given(tokenService.reissueToken(requestRefreshToken)).willReturn(serviceResult);

			ResponseCookie responseCookie = ResponseCookie.from(CookieProvider.REFRESH_TOKEN_COOKIE, newRefreshToken)
				.path("/")
				.httpOnly(true)
				.build();
			given(cookieProvider.createRefreshTokenCookie(newRefreshToken)).willReturn(responseCookie);

			TokenRefreshResponse responseDto = TokenRefreshResponse.builder()
				.accessToken(newAccessToken)
				.expiresIn(expiresIn)
				.build();

			ApiResponse<TokenRefreshResponse> expectedResponse = ApiResponse.ok("토큰이 재발급되었습니다.", responseDto);

			// when & then
			mockMvc.perform(
					post(DEFAULT_URL)
						.cookie(new Cookie(CookieProvider.REFRESH_TOKEN_COOKIE, requestRefreshToken))
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("SUCCESS"))
				.andExpect(jsonPath("$.message").value("토큰이 재발급되었습니다."))
				.andExpect(jsonPath("$.data.accessToken").value(newAccessToken))
				.andExpect(jsonPath("$.data.expiresIn").value(expiresIn))
				.andExpect(jsonPath("$.data.refreshToken").doesNotExist())
				.andDo(
					restDocsFactory.success(
						"token-refresh-success",
						SUMMARY,
						DESCRIPTION,
						"Token",
						null,
						expectedResponse
					)
				);
		}

		@Test
		void 실패_Case1_쿠키_누락() throws Exception {
			// when & then
			mockMvc.perform(post(DEFAULT_URL))
				.andExpect(status().isBadRequest())
				.andDo(
					restDocsFactory.failure(
						"token-refresh-fail-missing-cookie",
						SUMMARY,
						DESCRIPTION,
						"Token",
						null,
						ApiResponse.fail(ErrorCode.INVALID_INPUT)
					)
				);
		}

		@Test
		void 실패_Case2_인증정보_유효하지_않음() throws Exception {
			// given
			final String forgedToken = "forged.refresh.token";
			given(tokenService.reissueToken(forgedToken))
				.willThrow(new CustomException(ErrorCode.INVALID_TOKEN));

			// when & then
			mockMvc.perform(
					post(DEFAULT_URL)
						.cookie(new Cookie(CookieProvider.REFRESH_TOKEN_COOKIE, forgedToken))
				)
				.andExpect(status().isUnauthorized())
				.andDo(
					restDocsFactory.failure(
						"token-refresh-fail-invalid-token",
						SUMMARY,
						DESCRIPTION,
						"Token",
						null,
						ApiResponse.fail(ErrorCode.INVALID_TOKEN)
					)
				);
		}

		@Test
		void 실패_Case3_인증_만료() throws Exception {
			// given
			final String expiredToken = "expired.refresh.token";
			given(tokenService.reissueToken(expiredToken))
				.willThrow(new CustomException(ErrorCode.TOKEN_EXPIRED));

			// when & then
			mockMvc.perform(
					post(DEFAULT_URL)
						.cookie(new Cookie(CookieProvider.REFRESH_TOKEN_COOKIE, expiredToken))
				)
				.andExpect(status().isUnauthorized())
				.andDo(
					restDocsFactory.failure(
						"token-refresh-fail-token-expired",
						SUMMARY,
						DESCRIPTION,
						"Token",
						null,
						ApiResponse.fail(ErrorCode.TOKEN_EXPIRED)
					)
				);
		}

		@Test
		void 실패_Case4_서버_오류() throws Exception {
			// given
			final String validToken = "valid.refresh.token";
			given(tokenService.reissueToken(validToken))
				.willThrow(new RuntimeException("Redis Connection Error"));

			// when & then
			mockMvc.perform(
					post(DEFAULT_URL)
						.cookie(new Cookie(CookieProvider.REFRESH_TOKEN_COOKIE, validToken))
				)
				.andExpect(status().isInternalServerError())
				.andDo(
					restDocsFactory.failure(
						"token-refresh-fail-server-error",
						SUMMARY,
						DESCRIPTION,
						"Token",
						null,
						ApiResponse.fail(ErrorCode.ERROR_SYSTEM)
					)
				);
		}
	}
}
