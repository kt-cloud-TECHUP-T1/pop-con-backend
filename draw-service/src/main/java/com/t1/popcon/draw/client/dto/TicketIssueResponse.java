package com.t1.popcon.draw.client.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record TicketIssueResponse(
    Long ticketId,
    Long userId,
    Long popupId,
    String ticketNumber,
    String reservationNo,
    String status,
    String sourceType,
    Long sourceId,
    LocalDate entryDate,
    LocalTime entryTime,
    LocalDateTime issuedAt
) {
}
