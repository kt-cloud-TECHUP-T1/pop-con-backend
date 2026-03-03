package com.t1.popcon.auth.token.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
		throws IOException {

		// 1. JwtFilter에서 저장한 에러 코드를 꺼내옵니다.
		ErrorCode errorCode = (ErrorCode) request.getAttribute("exception");

		// 2. 만약 필터를 거치지 않고 직접 접근했을 경우를 대비해 기본값을 설정합니다.
		if (errorCode == null) {
			errorCode = ErrorCode.INVALID_TOKEN;
		}

		// 3. ApiResponse.fail()을 사용하여 표준화된 응답 객체를 만듭니다.
		ApiResponse<Void> apiResponse = ApiResponse.fail(errorCode);

		// 4. HttpServletResponse에 직접 JSON 정보를 입력합니다.
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.setStatus(errorCode.getStatus().value());

		// JSON 문자열로 변환하여 출력합니다.
		response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
	}
}