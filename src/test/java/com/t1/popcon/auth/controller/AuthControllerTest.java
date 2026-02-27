package com.t1.popcon.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.t1.popcon.auth.dto.AuthRequest;
import com.t1.popcon.auth.dto.AuthResponse;
import com.t1.popcon.auth.service.AuthService;
import com.t1.popcon.support.AbstractRestDocsTest;
import com.t1.popcon.support.RestDocsFactory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean; // 👈 변경된 임포트

import java.time.LocalDateTime;

@ActiveProfiles("test")
class AuthControllerTest extends AbstractRestDocsTest {

	private static final String DEFAULT_URL = "/auth";

	@Autowired
	private RestDocsFactory restDocsFactory;

	@MockitoBean
	private AuthService authService;

	@Nested
	class 회원가입_API {

		@Test
		void 성공() throws Exception {
			// given
			AuthRequest.Signup request = new AuthRequest.Signup(
				"test_register_token_1234",
				new AuthRequest.Agreements(true, true, true, true)
			);

			AuthResponse.Signup response = new AuthResponse.Signup(
				1L, "홍길동", "access_token", "refresh_token", LocalDateTime.now()
			);

			given(authService.signup(any())).willReturn(response);

			// when & then
			mockMvc.perform(
					restDocsFactory.createRequest(
						DEFAULT_URL + "/signup",
						request,
						HttpMethod.POST,
						objectMapper
					)
				)
				.andExpect(status().isOk())
				.andDo(
					restDocsFactory.success(
						"auth-signup",
						"회원가입 및 약관 동의",
						"본인인증 후 발급된 임시 토큰과 약관 동의 정보를 기반으로 회원가입을 진행합니다.",
						"Auth",
						AuthRequest.Signup.class,
						AuthResponse.Signup.class
					)
				);
		}
	}
}