package com.t1.popcon.queue.dto.vqa;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VqaSessionStartRequest(
    @JsonProperty("user_agent") String userAgent
) {
    public static VqaSessionStartRequest empty() {
        return new VqaSessionStartRequest("");
    }
}
