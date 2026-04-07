package com.t1.popcon.queue.dto.vqa;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VqaNextQuestionResponse(
    @JsonProperty("video") VideoInfo video,
    @JsonProperty("question") QuestionInfo question,
    @JsonProperty("is_exempt") Boolean isExempt // 면제 여부 필드 추가
) {
    public record VideoInfo(
        @JsonProperty("id") Long id,
        @JsonProperty("title") String title
    ) {}

    public record QuestionInfo(
        @JsonProperty("id") Long id,
        @JsonProperty("text") String text
    ) {}
}
