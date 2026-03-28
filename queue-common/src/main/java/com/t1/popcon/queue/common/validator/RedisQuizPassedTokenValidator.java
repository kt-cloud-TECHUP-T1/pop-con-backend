package com.t1.popcon.queue.common.validator;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.queue.QuizPassedTokenInfo;
import com.t1.popcon.common.queue.QuizPassedTokenValidator;
import com.t1.popcon.queue.common.domain.PhaseType;
import com.t1.popcon.queue.common.redis.QueueActiveRepository;
import com.t1.popcon.queue.common.redis.QueueRedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Redis 기반 퀴즈 통과 토큰 검증 구현체
 * - common 모듈의 QuizPassedTokenValidator 인터페이스 구현
 * - queue:quiz-passed-token:{token} HASH 조회 후 phaseType/phaseId/userId 파싱
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisQuizPassedTokenValidator implements QuizPassedTokenValidator {

    private final QueueActiveRepository queueActiveRepository;

    @Override
    public Optional<QuizPassedTokenInfo> validate(String token) {
        Map<Object, Object> hash = queueActiveRepository.getQuizPassedTokenHash(token);

        // Redis 미존재(TTL 만료 포함) 또는 필드 누락
        if (hash == null || hash.isEmpty()) {
            log.warn("[QuizPassedToken] 토큰 없음 또는 만료 - token={}", mask(token));
            return Optional.empty();
        }

        String phaseType = (String) hash.get(QueueRedisKeys.FIELD_PHASE_TYPE);
        String phaseIdStr = (String) hash.get(QueueRedisKeys.FIELD_PHASE_ID);
        String userIdStr  = (String) hash.get(QueueRedisKeys.FIELD_USER_ID);

        if (phaseType == null || phaseIdStr == null || userIdStr == null) {
            log.warn("[QuizPassedToken] 필드 누락 - token={}", mask(token));
            return Optional.empty();
        }

        try {
            // phaseType 유효성 검증 — Redis 데이터 오염 방어 (DRAW / AUCTION 외 값 차단)
            PhaseType resolvedPhaseType = PhaseType.from(phaseType);
            long phaseId = Long.parseLong(phaseIdStr);
            long userId  = Long.parseLong(userIdStr);
            return Optional.of(new QuizPassedTokenInfo(resolvedPhaseType.getValue(), phaseId, userId));
        } catch (CustomException e) {
            // PhaseType.from() 실패 — Redis에 잘못된 phaseType이 저장된 경우
            log.warn("[QuizPassedToken] 유효하지 않은 phaseType - token={}", mask(token));
            return Optional.empty();
        } catch (NumberFormatException e) {
            log.error("[QuizPassedToken] 필드 파싱 실패 - token={}", mask(token), e);
            return Optional.empty();
        }
    }

    /** 토큰 앞 4자리만 노출하여 마스킹 */
    private static String mask(String token) {
        if (token == null || token.length() <= 4) return "***";
        return token.substring(0, 4) + "...";
    }
}
