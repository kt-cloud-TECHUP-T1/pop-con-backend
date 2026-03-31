package com.t1.popcon.auction.bid.client.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record TicketIssueRequest(
    Long userId,
    Long popupId,
    String sourceType,
    Long sourceId,
    String reservationNo,
    LocalDate entryDate,
    LocalTime entryTime
) {
}
