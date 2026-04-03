package com.t1.popcon.queue.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VqaUserSubmitRequest(
    @NotBlank String vqaSessionId, // 우리 서버용 세션 UUID
    @NotNull Long videoId,
    @NotNull Long questionId,
    @NotBlank String userAnswer,
    @NotNull Double totalTime
) {}
