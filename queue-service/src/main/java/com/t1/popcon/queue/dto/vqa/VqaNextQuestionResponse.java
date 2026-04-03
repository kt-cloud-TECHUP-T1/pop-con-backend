package com.t1.popcon.queue.dto.vqa;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VqaNextQuestionResponse(
    @JsonProperty("video") VideoInfo video,
    @JsonProperty("question") QuestionInfo question
) {
    public record VideoInfo(
        @JsonProperty("id") Long id,
        @JsonProperty("title") String title
    ) {}

    public record QuestionInfo(
        @JsonProperty("id") Long id,
        @JsonProperty("text") String text,
        @JsonProperty("correct_answer") String correctAnswer
    ) {}
}
