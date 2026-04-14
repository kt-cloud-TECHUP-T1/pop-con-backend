package com.t1.popcon.queue.dto.vqa;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VqaSessionStartResponse(
    @JsonProperty("session_id") Long sessionId
) {}
