package com.t1.popcon.common.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.common.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * X-Quiz-Passed-Token 헤더 검증 필터
 * - 퀴즈 통과 사용자만 보호 대상 API(드로우 응모, 경매 입찰 등) 접근 허용
 * - 검증 성공 시 request attribute에 QuizPassedTokenInfo 저장
 */
@Slf4j
@RequiredArgsConstructor
public class QuizPassedTokenFilter extends OncePerRequestFilter {

	public static final String QUIZ_PASSED_TOKEN_HEADER = "X-Quiz-Passed-Token";
	public static final String QUIZ_PASSED_TOKEN_INFO_ATTRIBUTE = "quizPassedTokenInfo";

	private final QuizPassedTokenValidator quizPassedTokenValidator;
	private final ObjectMapper objectMapper;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return HttpMethod.OPTIONS.name().equals(request.getMethod());
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {

		// 헤더에서 토큰 추출
		String token = request.getHeader(QUIZ_PASSED_TOKEN_HEADER);

		if (!StringUtils.hasText(token)) {
			log.warn("[QuizPassedTokenFilter] X-Quiz-Passed-Token 헤더 누락 - uri={}", request.getRequestURI());
			sendErrorResponse(response, ErrorCode.QUIZ_PASSED_TOKEN_MISSING);
			return;
		}

		// Redis 기반 토큰 검증
		Optional<QuizPassedTokenInfo> tokenInfo;
		try {
			tokenInfo = quizPassedTokenValidator.validate(token);
		} catch (Exception e) {
			log.error("[QuizPassedTokenFilter] 토큰 검증 중 오류 - uri={}", request.getRequestURI(), e);
			sendErrorResponse(response, ErrorCode.ERROR_SYSTEM);
			return;
		}

		if (tokenInfo.isEmpty()) {
			log.warn("[QuizPassedTokenFilter] 유효하지 않은 토큰 - uri={}", request.getRequestURI());
			sendErrorResponse(response, ErrorCode.QUIZ_PASSED_TOKEN_INVALID);
			return;
		}

		// 검증 통과 — 컨트롤러에서 사용할 수 있도록 저장
		log.debug("[QuizPassedTokenFilter] 검증 통과 - userId={}, phaseType={}, phaseId={}",
			tokenInfo.get().userId(), tokenInfo.get().phaseType(), tokenInfo.get().phaseId());
		request.setAttribute(QUIZ_PASSED_TOKEN_INFO_ATTRIBUTE, tokenInfo.get());
		filterChain.doFilter(request, response);
	}

	private void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
		response.setStatus(errorCode.getStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getWriter(), ApiResponse.fail(errorCode));
	}
}
