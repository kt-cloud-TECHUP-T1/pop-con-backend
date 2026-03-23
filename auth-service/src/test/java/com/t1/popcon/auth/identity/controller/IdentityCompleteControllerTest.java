package com.t1.popcon.auth.identity.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.t1.popcon.auth.common.config.FrontendProperties;
import com.t1.popcon.auth.identity.dto.IdentityCompleteRequest;
import com.t1.popcon.auth.identity.dto.IdentityCompleteResponse;
import com.t1.popcon.auth.identity.service.IdentityCompleteService;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.support.AbstractRestDocsTest;
import com.t1.popcon.support.RestDocsFactory;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@ActiveProfiles("test")
class IdentityCompleteControllerTest extends AbstractRestDocsTest {

	private static final String DEFAULT_URL = "/auth/identity/complete";
	private static final String VALID_REGISTER_TOKEN = "valid_register_token_mock";
	private static final String VALID_VERIFICATION_ID = "imp_1234567890";

	@Autowired
	private RestDocsFactory restDocsFactory;

	@MockitoBean
	private IdentityCompleteService identityCompleteService;

	@MockitoBean
	private FrontendProperties frontendProperties;

	@BeforeEach
	void setUp() {
		// 쿠키 생성을 위한 FrontendProperties 기본 모킹
		given(frontendProperties.isCookieSecure()).willReturn(true);
		given(frontendProperties.cookieDomain()).willReturn("localhost");
		given(frontendProperties.cookieSameSite()).willReturn("Lax");
	}

	@Nested
	class 본인인증_완료_API {

		private static final String SUMMARY = "포트원 본인인증 완료 처리";
		private static final String DESCRIPTION = """
            포트원 본인인증 완료 후 결과를 검증하고, 기존/신규 회원 여부에 따라 다음 단계를 분기합니다.
            
            [헤더 파라미터]
            - X-Device-Id: (선택) 기기 식별값
            
            [쿠키 파라미터]
            - register_token: (필수) 가입 진행 토큰

            [에러 케이스]
            - 400 (C001): 본인인증 식별자 누락
            - 401 (A001): 가입 세션(register_token) 만료 또는 누락
            - 400 (I001/I002): 본인인증 조회/검증 실패
            - 403 (J001): 만 14세 미만 가입 제한
            """;

		@Test
		void 성공_신규_회원() throws Exception {
			// given
			IdentityCompleteRequest.Complete requestDto = new IdentityCompleteRequest.Complete(VALID_VERIFICATION_ID);
			IdentityCompleteResponse.NewUserComplete responseDto = IdentityCompleteResponse.NewUserComplete.terms();

			ApiResponse<IdentityCompleteResponse> expectedResponse =
					ApiResponse.ok("본인인증이 완료되었습니다. 약관에 동의해주세요.", responseDto);

			given(identityCompleteService.complete(any(), anyString()))
					.willReturn(IdentityCompleteService.IdentityCompleteResult.newUser(responseDto));

			// when & then
			performPost(DEFAULT_URL, requestDto, new Cookie("register_token", VALID_REGISTER_TOKEN))
					.andExpect(status().isOk())
					.andDo(
							restDocsFactory.success(
									"identity-complete-new-user-success",
									SUMMARY,
									DESCRIPTION,
									"Auth",
									requestDto,
									expectedResponse
							)
					);
		}

		@Test
		void 성공_기존_회원() throws Exception {
			// given
			IdentityCompleteRequest.Complete requestDto = new IdentityCompleteRequest.Complete(VALID_VERIFICATION_ID);
			IdentityCompleteResponse.ExistingUserComplete responseDto =
					IdentityCompleteResponse.ExistingUserComplete.of(105L, "access_token_mock");

			ApiResponse<IdentityCompleteResponse> expectedResponse =
					ApiResponse.ok("계정 연결 및 로그인이 완료되었습니다.", responseDto);

			given(identityCompleteService.complete(any(), anyString()))
					.willReturn(IdentityCompleteService.IdentityCompleteResult.existingUser(
							responseDto, "refresh_token_mock", 1209600L
					));

			// when & then
			performPost(DEFAULT_URL, requestDto, new Cookie("register_token", VALID_REGISTER_TOKEN))
					.andExpect(status().isOk())
					.andExpect(cookie().exists("refresh_token"))
					.andExpect(cookie().maxAge("register_token", 0)) // register_token 삭제 검증
					.andDo(
							restDocsFactory.success(
									"identity-complete-existing-user-success",
									SUMMARY,
									DESCRIPTION,
									"Auth",
									requestDto,
									expectedResponse
							)
					);
		}

		@Test
		void 실패_register_token_누락() throws Exception {
			// given
			IdentityCompleteRequest.Complete requestDto = new IdentityCompleteRequest.Complete(VALID_VERIFICATION_ID);
			ApiResponse<Void> expectedResponse = ApiResponse.fail(ErrorCode.REGISTER_TOKEN_EXPIRED);

			// when & then: 쿠키 없이 요청
			performPost(DEFAULT_URL, requestDto, null)
					.andExpect(status().isUnauthorized()) // A001이 401 매핑이라고 가정
					.andDo(
							restDocsFactory.failure(
									"identity-complete-fail-missing-cookie",
									SUMMARY,
									DESCRIPTION,
									"Auth",
									requestDto,
									expectedResponse
							)
					);
		}

		@Test
		void 실패_잘못된_입력값() throws Exception {
			// given
			IdentityCompleteRequest.Complete requestDto = new IdentityCompleteRequest.Complete(""); // 빈 식별자
			ApiResponse<?> expectedResponse = invalidInput("identityVerificationId", "본인인증 식별자가 필요합니다.");

			// when & then
			performPost(DEFAULT_URL, requestDto, new Cookie("register_token", VALID_REGISTER_TOKEN))
					.andExpect(status().isBadRequest())
					.andDo(
							restDocsFactory.failure(
									"identity-complete-fail-invalid-input",
									SUMMARY,
									DESCRIPTION,
									"Auth",
									requestDto,
									expectedResponse
							)
					);
		}
	}

	/**
	 * POST 요청을 수행하고 쿠키를 세팅하는 헬퍼 메서드
	 */
	private ResultActions performPost(String url, Object body, Cookie cookie) throws Exception {
		MockHttpServletRequestBuilder requestBuilder = (MockHttpServletRequestBuilder) restDocsFactory.createRequest(
				url,
				body,
				HttpMethod.POST,
				objectMapper
		);

		if (cookie != null) {
			requestBuilder.cookie(cookie);
		}

		return mockMvc.perform(requestBuilder);
	}

	private ApiResponse<?> invalidInput(String field, String message) {
		return ApiResponse.fail(ErrorCode.INVALID_INPUT, Map.of(field, message));
	}
}