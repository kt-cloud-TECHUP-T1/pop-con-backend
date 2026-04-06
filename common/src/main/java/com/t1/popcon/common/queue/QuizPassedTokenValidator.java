package com.t1.popcon.common.queue;

import java.util.Optional;

/**
 * 퀴즈 통과 토큰 검증 인터페이스
 * - 구현체: queue-common 모듈의 RedisQuizPassedTokenValidator
 */
public interface QuizPassedTokenValidator {

	Optional<QuizPassedTokenInfo> validate(String token);
}
