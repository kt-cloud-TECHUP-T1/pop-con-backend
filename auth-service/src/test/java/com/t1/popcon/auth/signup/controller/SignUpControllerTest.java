package com.t1.popcon.auth.signup.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.cookies.CookieDocumentation.cookieWithName;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.t1.popcon.auth.signup.dto.SignUpRequest;
import com.t1.popcon.auth.signup.dto.SignUpResponse;
import com.t1.popcon.auth.signup.service.SignUpService;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.support.AbstractRestDocsTest;
import com.t1.popcon.support.RestDocsFactory;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.Map;

@ActiveProfiles("test")
class SignUpControllerTest extends AbstractRestDocsTest {

	private static final String DEFAULT_URL = "/auth";

	@Autowired
	private RestDocsFactory restDocsFactory;

	@MockitoBean
	private SignUpService signUpService;

	@Nested
	class 회원가입_API {

		private static final String SUMMARY = "회원가입 및 약관 동의";
		private static final String DESCRIPTION = """
			본인인증 후 발급된 임시 토큰과 약관 동의 정보를 기반으로 회원가입을 진행합니다.
			
			[에러 케이스]
			- 400 (C001): 입력값이 올바르지 않음 (필수 약관 누락 등)
			- 400 (U002): 가입 세션의 소셜 정보 누락
			- 401 (A001): 회원가입 세션 만료
			- 401 (A002): 인증 정보가 유효하지 않음
			- 409 (J002): 이미 가입이 완료된 회원
			- 500 (S001): 서버 오류
			""";

		@Test
		void 성공() throws Exception {
			// given
			SignUpRequest.Signup request = new SignUpRequest.Signup(
				new SignUpRequest.Agreements(true, true, true, true)
			);

			SignUpResponse.Signup responseDto = new SignUpResponse.Signup(
				1L, "홍길동", "access_token", LocalDateTime.now()
			);

			ApiResponse<SignUpResponse.Signup> expectedResponse = ApiResponse.ok("약관 동의 및 회원가입이 완료되었습니다.", responseDto);

			given(signUpService.signup(any(), any())).willReturn(new SignUpService.SignupResult(
				new com.t1.popcon.auth.signup.client.dto.UserCreateResponse(1L, "홍길동", "test@test.com"),
				"access_token",
				"refresh_token"
			));

			// when & then
			mockMvc.perform(
					restDocsFactory.createRequest(
						DEFAULT_URL + "/signup",
						request,
						HttpMethod.POST,
						objectMapper
					).cookie(new Cookie("register_token", "test_register_token_1234"))
				)
				.andExpect(status().isOk())
				.andDo(
					restDocsFactory.success(
						"auth-signup",
						SUMMARY,
						DESCRIPTION,
						"Auth",
						request,
						expectedResponse,
						cookieWithName("register_token").description("회원가입 세션 유지를 위한 임시 토큰")
					)
				);
		}

		@Test
		void 실패_Case1_입력값_오류() throws Exception {
			// given
			// 의도적으로 잘못된 요청값 생성 (필수 약관 미동의)
			SignUpRequest.Signup request = new SignUpRequest.Signup(
				new SignUpRequest.Agreements(false, false, false, null)
			);

			// REST Docs 문서화를 위한 예상 에러 응답 객체 구성
			Map<String, String> fieldErrors = Map.of(
				"agreements.isPrivacyPolicyAgreed", "필수 동의 항목입니다.",
				"agreements.isIdentifierPolicyAgreed", "필수 동의 항목입니다.",
				"agreements.isServicePolicyAgreed", "필수 동의 항목입니다.",
				"agreements.isMarketingAgreed", "선택 약관 항목이 누락되었습니다."
			);
			ApiResponse<Map<String, String>> expectedResponse = ApiResponse.fail(ErrorCode.INVALID_INPUT, fieldErrors);

			// when & then
			mockMvc.perform(
					restDocsFactory.createRequest(
						DEFAULT_URL + "/signup",
						request,
						HttpMethod.POST,
						objectMapper
					).cookie(new Cookie("register_token", "test_register_token_1234"))
				)
				.andExpect(status().isBadRequest())
				.andDo(
					restDocsFactory.failure(
						"auth-signup-fail-invalid-input",
						SUMMARY,
						DESCRIPTION,
						"Auth",
						request,
						expectedResponse,
						cookieWithName("register_token").description("회원가입 세션 유지를 위한 임시 토큰")
					)
				);
		}

		@Test
		void 실패_Case2_이미_가입된_회원() throws Exception {
			// given
			SignUpRequest.Signup request = new SignUpRequest.Signup(new SignUpRequest.Agreements(true, true, true, true));
			ApiResponse<Void> expectedResponse = ApiResponse.fail(ErrorCode.ALREADY_SIGNED_UP);

			given(signUpService.signup(any(), any())).willThrow(new CustomException(ErrorCode.ALREADY_SIGNED_UP));

			// when & then
			mockMvc.perform(restDocsFactory.createRequest(DEFAULT_URL + "/signup", request, HttpMethod.POST, objectMapper)
					.cookie(new Cookie("register_token", "valid_token")))
				.andExpect(status().isConflict())
				.andDo(
					restDocsFactory.failure(
						"auth-signup-fail-already-signed-up",
						SUMMARY,
						DESCRIPTION,
						"Auth",
						request,
						expectedResponse,
						cookieWithName("register_token").description("회원가입 세션 유지를 위한 임시 토큰")
					)
				);
		}

		@Test
		void 실패_Case3_가입세션_토큰_만료() throws Exception {
			// given
			SignUpRequest.Signup request = new SignUpRequest.Signup(new SignUpRequest.Agreements(true, true, true, true));
			ApiResponse<Void> expectedResponse = ApiResponse.fail(ErrorCode.REGISTER_TOKEN_EXPIRED);

			given(signUpService.signup(any(), any())).willThrow(new CustomException(ErrorCode.REGISTER_TOKEN_EXPIRED));

			// when & then
			mockMvc.perform(restDocsFactory.createRequest(DEFAULT_URL + "/signup", request, HttpMethod.POST, objectMapper)
					.cookie(new Cookie("register_token", "expired_token")))
				.andExpect(status().isUnauthorized())
				.andDo(
					restDocsFactory.failure(
						"auth-signup-fail-token-expired",
						SUMMARY,
						DESCRIPTION,
						"Auth",
						request,
						expectedResponse,
						cookieWithName("register_token").description("회원가입 세션 유지를 위한 임시 토큰")
					)
				);
		}

		@Test
		void 실패_Case4_인증정보_유효하지_않음() throws Exception {
			// given
			SignUpRequest.Signup request = new SignUpRequest.Signup(new SignUpRequest.Agreements(true, true, true, true));
			ApiResponse<Void> expectedResponse = ApiResponse.fail(ErrorCode.INVALID_TOKEN);

			given(signUpService.signup(any(), any())).willThrow(new CustomException(ErrorCode.INVALID_TOKEN));

			// when & then
			mockMvc.perform(restDocsFactory.createRequest(DEFAULT_URL + "/signup", request, HttpMethod.POST, objectMapper)
					.cookie(new Cookie("register_token", "invalid_token")))
				.andExpect(status().isUnauthorized())
				.andDo(
					restDocsFactory.failure(
						"auth-signup-fail-invalid-token",
						SUMMARY,
						DESCRIPTION,
						"Auth",
						request,
						expectedResponse,
						cookieWithName("register_token").description("회원가입 세션 유지를 위한 임시 토큰")
					)
				);
		}

		@Test
		void 실패_Case5_소셜정보_누락() throws Exception {
			// given
			SignUpRequest.Signup request = new SignUpRequest.Signup(new SignUpRequest.Agreements(true, true, true, true));
			ApiResponse<Void> expectedResponse = ApiResponse.fail(ErrorCode.SOCIAL_INFO_MISSING);

			given(signUpService.signup(any(), any())).willThrow(new CustomException(ErrorCode.SOCIAL_INFO_MISSING));

			// when & then
			mockMvc.perform(restDocsFactory.createRequest(DEFAULT_URL + "/signup", request, HttpMethod.POST, objectMapper)
					.cookie(new Cookie("register_token", "no_social_info_token")))
				.andExpect(status().isBadRequest())
				.andDo(
					restDocsFactory.failure(
						"auth-signup-fail-social-info-missing",
						SUMMARY,
						DESCRIPTION,
						"Auth",
						request,
						expectedResponse,
						cookieWithName("register_token").description("회원가입 세션 유지를 위한 임시 토큰")
					)
				);
		}

		@Test
		void 실패_Case6_서버_오류() throws Exception {
			// given
			SignUpRequest.Signup request = new SignUpRequest.Signup(new SignUpRequest.Agreements(true, true, true, true));
			ApiResponse<Void> expectedResponse = ApiResponse.fail(ErrorCode.ERROR_SYSTEM);

			// 예기치 않은 서버 내부 오류(RuntimeException) 발생 시뮬레이션
			given(signUpService.signup(any(), any())).willThrow(new RuntimeException("DB Connection Timeout 등 예기치 않은 예외"));

			// when & then
			mockMvc.perform(restDocsFactory.createRequest(DEFAULT_URL + "/signup", request, HttpMethod.POST, objectMapper)
					.cookie(new Cookie("register_token", "valid_token")))
				.andExpect(status().isInternalServerError())
				.andDo(
					restDocsFactory.failure(
						"auth-signup-fail-server-error",
						SUMMARY,
						DESCRIPTION,
						"Auth",
						request,
						expectedResponse,
						cookieWithName("register_token").description("회원가입 세션 유지를 위한 임시 토큰")
					)
				);
		}
	}
}