package com.t1.popcon.queue.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record VqaUserSubmitRequest(
    @NotBlank
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "유효하지 않은 세션 형식입니다.")
    String vqaSessionId, // 우리 서버용 세션 UUID

    @NotNull
    @Positive(message = "비디오 ID는 양수여야 합니다.")
    Long videoId,

    @NotNull
    @Positive(message = "질문 ID는 양수여야 합니다.")
    Long questionId,

    @NotBlank
    @Size(min = 1, max = 500, message = "답변은 1자 이상 500자 이내여야 합니다.")
    String userAnswer,

    @NotNull
    @Positive(message = "소요 시간은 0보다 커야 합니다.")
    Double totalTime
) {}
