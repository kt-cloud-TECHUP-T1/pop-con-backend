package com.t1.popcon.draw.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Builder;

@Builder
public record DrawEntryDetailResponse(
    Long drawEntryId,
    Long drawId,
    String popupTitle,
    String popupAddress,
    String popupThumbnail,
    LocalDate entryDate,
    LocalTime entryTime,
    String userName,
    String userPhoneNumber,
    LocalDateTime paidAt,
    String status,
    LocalDateTime ticketIssuedAt
) {
}
