package com.t1.popcon.draw.client.dto;

public record TicketIssueResponse(
    Long ticketId,
    String ticketNumber,
    String reservationNo,
    String status,
    String sourceType,
    Long sourceId
) {
}
