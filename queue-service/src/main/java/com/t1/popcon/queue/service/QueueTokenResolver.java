package com.t1.popcon.queue.service;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.queue.common.domain.PhaseType;
import com.t1.popcon.queue.common.redis.QueueActiveRepository;
import com.t1.popcon.queue.common.redis.QueueRedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * queueToken → (phaseType, phaseId, userId) 역조회 공통 헬퍼
 * - Redis HASH 조회 후 필드 파싱 + null/형식 검증
 * - getStatus(), leave() 등에서 중복 제거 목적
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueueTokenResolver {

    private final QueueActiveRepository activeRepository;

    /**
     * queueToken 역조회 결과
     * - 불변 값 객체 — 파싱 성공 시에만 생성
     */
    public record TokenInfo(String phaseType, long phaseId, long userId) {
    }

    /**
     * queueToken으로 사용자 정보 역조회
     * - 토큰 없음/만료 → QUEUE_TOKEN_INVALID
     * - 필드 누락/형식 오류 → QUEUE_TOKEN_INVALID + WARN 로그
     */
    public TokenInfo resolve(String queueToken) {
        Map<Object, Object> hash = activeRepository.getQueueTokenHash(queueToken);
        if (hash == null || hash.isEmpty()) {
            log.warn("[Queue] 토큰 없음 또는 만료 - token={}", mask(queueToken));
            throw new CustomException(ErrorCode.QUEUE_TOKEN_INVALID);
        }

        Object phaseTypeObj = hash.get(QueueRedisKeys.FIELD_PHASE_TYPE);
        Object phaseIdObj = hash.get(QueueRedisKeys.FIELD_PHASE_ID);
        Object userIdObj = hash.get(QueueRedisKeys.FIELD_USER_ID);

        // 필드 누락 검증
        if (phaseTypeObj == null || phaseIdObj == null || userIdObj == null) {
            log.warn("[Queue] 토큰 HASH 필드 누락 - token={}", mask(queueToken));
            throw new CustomException(ErrorCode.QUEUE_TOKEN_INVALID);
        }

        // phaseType 검증 — Redis 데이터 오염 방어 (DRAW/AUCTION 외 값 차단)
        String phaseType;
        try {
            phaseType = PhaseType.from(phaseTypeObj.toString()).getValue();
        } catch (CustomException e) {
            log.warn("[Queue] 토큰 HASH phaseType 오염 - token={}", mask(queueToken));
            throw new CustomException(ErrorCode.QUEUE_TOKEN_INVALID);
        }

        // 숫자 필드 파싱 및 범위 검증
        try {
            long phaseId = Long.parseLong(phaseIdObj.toString());
            long userId = Long.parseLong(userIdObj.toString());

            if (phaseId <= 0 || userId <= 0) {
                log.warn("[Queue] 토큰 HASH 필드값 범위 오류 - token={}", mask(queueToken));
                throw new CustomException(ErrorCode.QUEUE_TOKEN_INVALID);
            }

            return new TokenInfo(phaseType, phaseId, userId);
        } catch (NumberFormatException e) {
            log.warn("[Queue] 토큰 HASH 필드 파싱 실패 - token={}", mask(queueToken));
            throw new CustomException(ErrorCode.QUEUE_TOKEN_INVALID);
        }
    }

    /** 토큰 마스킹 (앞 4자리 + "...") */
    static String mask(String token) {
        if (token == null || token.length() <= 4) return "***";
        return token.substring(0, 4) + "...";
    }
}
