package com.t1.popcon.queue.dto.vqa;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VqaSubmitRequest(
    @JsonProperty("session_id") Long sessionId,
    @JsonProperty("video_id") Long videoId,
    @JsonProperty("question_id") Long questionId,
    @JsonProperty("user_answer") String userAnswer,
    @JsonProperty("total_time") Double totalTime
) {}
