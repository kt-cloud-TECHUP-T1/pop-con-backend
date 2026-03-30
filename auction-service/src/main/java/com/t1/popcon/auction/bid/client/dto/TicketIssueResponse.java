package com.t1.popcon.auction.bid.client.dto;

public record TicketIssueResponse(
    Long ticketId,
    String status,
    String sourceType,
    Long sourceId
) {
}
