package com.t1.popcon.auth.token.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.t1.popcon.auth.token.dto.TokenRefreshRequest;
import com.t1.popcon.auth.token.dto.TokenRefreshResponse;
import com.t1.popcon.auth.token.service.TokenService;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.support.AbstractRestDocsTest;
import com.t1.popcon.support.RestDocsFactory;

@ActiveProfiles("test")
class TokenControllerTest extends AbstractRestDocsTest {

	private static final String DEFAULT_URL = "/auth/token/refresh";

	@Autowired
	private RestDocsFactory restDocsFactory;

	@MockitoBean
	private TokenService tokenService;

	@Nested
	class 토큰_재발급_API {

		private static final String SUMMARY = "토큰 재발급";
		private static final String DESCRIPTION = """
			Refresh Token을 사용하여 새로운 Access/Refresh Token을 발급받습니다.
			
			[에러 케이스]
			- 400 (C001): 입력값이 올바르지 않음 (refreshToken 누락/빈값)
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

			TokenRefreshRequest request = new TokenRefreshRequest(requestRefreshToken);
			TokenRefreshResponse responseDto = TokenRefreshResponse.builder()
				.accessToken(newAccessToken)
				.refreshToken(newRefreshToken)
				.expiresIn(expiresIn)
				.build();

			ApiResponse<TokenRefreshResponse> expectedResponse = ApiResponse.ok("토큰이 재발급되었습니다.", responseDto);

			given(tokenService.reissueToken(any(TokenRefreshRequest.class))).willReturn(responseDto);

			// when & then
			mockMvc.perform(
					restDocsFactory.createRequest(DEFAULT_URL, request, HttpMethod.POST, objectMapper)
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("SUCCESS"))
				.andExpect(jsonPath("$.message").value("토큰이 재발급되었습니다."))
				.andExpect(jsonPath("$.data.accessToken").value(newAccessToken))
				.andExpect(jsonPath("$.data.refreshToken").value(newRefreshToken))
				.andDo(
					restDocsFactory.success(
						"token-refresh-success",
						SUMMARY,
						DESCRIPTION,
						"Token",
						request,
						expectedResponse
					)
				);
		}

		@Test
		void 실패_Case1_입력값_오류() throws Exception {
			// given
			// @NotBlank 에러를 발생시키기 위해 빈 값("") 전달
			TokenRefreshRequest request = new TokenRefreshRequest("");

			Map<String, String> fieldErrors = Map.of("refreshToken", "refreshToken이 필요합니다.");
			ApiResponse<Map<String, String>> expectedResponse = ApiResponse.fail(ErrorCode.INVALID_INPUT, fieldErrors);

			// when & then
			mockMvc.perform(restDocsFactory.createRequest(DEFAULT_URL, request, HttpMethod.POST, objectMapper))
				.andExpect(status().isBadRequest())
				.andDo(
					restDocsFactory.failure(
						"token-refresh-fail-invalid-input",
						SUMMARY,
						DESCRIPTION,
						"Token",
						request,
						expectedResponse
					)
				);
		}

		@Test
		void 실패_Case2_인증정보_유효하지_않음() throws Exception {
			// given
			TokenRefreshRequest request = new TokenRefreshRequest("forged.refresh.token");
			ApiResponse<Void> expectedResponse = ApiResponse.fail(ErrorCode.INVALID_TOKEN);

			given(tokenService.reissueToken(any(TokenRefreshRequest.class)))
				.willThrow(new CustomException(ErrorCode.INVALID_TOKEN));

			// when & then
			mockMvc.perform(restDocsFactory.createRequest(DEFAULT_URL, request, HttpMethod.POST, objectMapper))
				.andExpect(status().isUnauthorized())
				.andDo(
					restDocsFactory.failure(
						"token-refresh-fail-invalid-token",
						SUMMARY,
						DESCRIPTION,
						"Token",
						request,
						expectedResponse
					)
				);
		}

		@Test
		void 실패_Case3_인증_만료() throws Exception {
			// given
			TokenRefreshRequest request = new TokenRefreshRequest("expired.refresh.token");
			ApiResponse<Void> expectedResponse = ApiResponse.fail(ErrorCode.TOKEN_EXPIRED);

			given(tokenService.reissueToken(any(TokenRefreshRequest.class)))
				.willThrow(new CustomException(ErrorCode.TOKEN_EXPIRED));

			// when & then
			mockMvc.perform(restDocsFactory.createRequest(DEFAULT_URL, request, HttpMethod.POST, objectMapper))
				.andExpect(status().isUnauthorized())
				.andDo(
					restDocsFactory.failure(
						"token-refresh-fail-token-expired",
						SUMMARY,
						DESCRIPTION,
						"Token",
						request,
						expectedResponse
					)
				);
		}

		@Test
		void 실패_Case4_서버_오류() throws Exception {
			// given
			TokenRefreshRequest request = new TokenRefreshRequest("valid.refresh.token");
			ApiResponse<Void> expectedResponse = ApiResponse.fail(ErrorCode.ERROR_SYSTEM);

			given(tokenService.reissueToken(any(TokenRefreshRequest.class)))
				.willThrow(new RuntimeException("Redis Connection Error"));

			// when & then
			mockMvc.perform(restDocsFactory.createRequest(DEFAULT_URL, request, HttpMethod.POST, objectMapper))
				.andExpect(status().isInternalServerError())
				.andDo(
					restDocsFactory.failure(
						"token-refresh-fail-server-error",
						SUMMARY,
						DESCRIPTION,
						"Token",
						request,
						expectedResponse
					)
				);
		}
	}
}