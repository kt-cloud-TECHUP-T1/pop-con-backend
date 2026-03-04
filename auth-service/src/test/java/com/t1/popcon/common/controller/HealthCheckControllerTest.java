package com.t1.popcon.common.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.t1.popcon.AuthApplication;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;

import com.t1.popcon.support.AbstractRestDocsTest;
import com.t1.popcon.support.RestDocsFactory;

@SpringBootTest(classes = AuthApplication.class)
@ActiveProfiles("test")
class HealthCheckControllerTest extends AbstractRestDocsTest {

	private static final String DEFAULT_URL = "/health";

	@Autowired
	private RestDocsFactory restDocsFactory;

	@Nested
	class 헬스_체크_API {

		@Test
		void 성공() throws Exception {
			// given
			// 헬스 체크 API는 요청(Request)과 응답(Response) DTO가 없으므로 생략합니다.

			// when & then
			mockMvc.perform(
					restDocsFactory.createRequest(
						DEFAULT_URL,
						null, // requestDto가 없으므로 null
						HttpMethod.GET,
						objectMapper
					)
				)
				.andExpect(status().isOk())
				.andExpect(content().string("Pop-Con Server is running"))
				.andDo(
					restDocsFactory.success(
						"health-check",     				// identifier
						"서버 헬스 체크",               				// summary (Swagger 제목)
						"팝콘 서버의 정상 작동 여부를 확인합니다.", 	// description
						"Common",                     			// tag
						null,                         			// requestDto
						null                          			// responseDto
					)
				);
		}
	}
}