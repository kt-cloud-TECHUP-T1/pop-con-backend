package com.t1.popcon.draw.dto.response;

import java.time.LocalTime;

public record DrawOptionResponse(
    Long optionId,
    LocalTime entryTime
) {
}