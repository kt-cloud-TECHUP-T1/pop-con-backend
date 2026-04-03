package com.t1.popcon.draw.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Builder;

@Builder
public record DrawEntryResultResponse(
    String vThumbnailUrl,
    String popupTitle,
    String popupAddress,
    LocalDate entryDate,
    LocalTime entryTime,
    String userName,
    String userPhoneNumber
) {
}
