package com.t1.popcon.ticket.dto.response;

import com.t1.popcon.ticket.domain.Ticket;
import com.t1.popcon.ticket.domain.TicketSourceType;
import com.t1.popcon.ticket.domain.TicketStatus;

public record TicketIssueResponse(
    Long ticketId,
    TicketStatus status,
    TicketSourceType sourceType,
    Long sourceId
) {

    public static TicketIssueResponse from(Ticket ticket) {
        return new TicketIssueResponse(
            ticket.getId(),
            ticket.getStatus(),
            ticket.getSourceType(),
            ticket.getSourceId()
        );
    }
}
