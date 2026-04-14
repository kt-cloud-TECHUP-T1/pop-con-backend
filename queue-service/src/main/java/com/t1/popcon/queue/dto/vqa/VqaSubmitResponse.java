package com.t1.popcon.queue.dto.vqa;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VqaSubmitResponse(
    @JsonProperty("submission_id") String submissionId,
    @JsonProperty("similarity_score") Double similarityScore,
    @JsonProperty("is_correct") Boolean isCorrect
) {}
