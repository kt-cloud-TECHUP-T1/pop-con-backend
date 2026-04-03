package com.t1.popcon.queue.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 퀴즈 제출 결과 응답 (재시도 정보 포함)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VqaSubmitResult(
    boolean isPass,
    Double score,
    String quizPassedToken,
    Integer remainAttempts // 남은 횟수 추가
) {
    public static VqaSubmitResult fail(Double score, int remainAttempts) {
        return new VqaSubmitResult(false, score, null, remainAttempts);
    }

    public static VqaSubmitResult success(Double score, String quizPassedToken) {
        return new VqaSubmitResult(true, score, quizPassedToken, null);
    }
}
