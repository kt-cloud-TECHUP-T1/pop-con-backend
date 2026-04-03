package com.t1.popcon.queue.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.t1.popcon.queue.dto.vqa.VqaNextQuestionResponse;

/**
 * 보안 퀴즈 시작 응답 (원샷 방식)
 * - isExempt: true면 퀴즈 면제 (바로 quizPassedToken 반환)
 * - isExempt: false면 퀴즈 진행 (vqaSessionId + firstQuestion 반환)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VqaStartResponse(
    boolean isExempt,
    String vqaSessionId,
    String quizPassedToken,
    VqaNextQuestionResponse firstQuestion
) {
    public static VqaStartResponse exempt(String quizPassedToken) {
        return new VqaStartResponse(true, null, quizPassedToken, null);
    }

    public static VqaStartResponse session(String vqaSessionId, VqaNextQuestionResponse firstQuestion) {
        return new VqaStartResponse(false, vqaSessionId, null, firstQuestion);
    }
}
