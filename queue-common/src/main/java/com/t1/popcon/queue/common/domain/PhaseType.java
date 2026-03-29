package com.t1.popcon.queue.common.domain;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 대기열 적용 대상 Phase 유형
 * - Redis 키 구성 시 {type} 세그먼트로 사용
 */
@Slf4j
@Getter
@RequiredArgsConstructor
public enum PhaseType {

    DRAW("draw"),
    AUCTION("auction");

    // Redis 키 세그먼트 값 (소문자)
    private final String value;

    /**
     * 문자열로 PhaseType 조회
     * - 대소문자 무관 (draw/DRAW 모두 허용)
     * - null·공백·미정의 값은 INVALID_INPUT CustomException
     */
    public static PhaseType from(String value) {
        if (value == null || value.isBlank()) {
            log.warn("[PhaseType] phaseType 누락");
            throw new CustomException(ErrorCode.INVALID_INPUT, "phaseType은 필수입니다.");
        }
        for (PhaseType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        log.warn("[PhaseType] 알 수 없는 phaseType - value={}", value);
        throw new CustomException(ErrorCode.INVALID_INPUT, "지원하지 않는 phaseType입니다: " + value);
    }
}
