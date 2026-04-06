package com.t1.popcon.ticket.dto.response;

import com.t1.popcon.ticket.domain.Ticket;
import com.t1.popcon.ticket.domain.TicketSourceType;
import com.t1.popcon.ticket.domain.TicketStatus;

public record TicketIssueResponse(
    Long ticketId,
    String ticketNumber,
    String reservationNo,
    TicketStatus status,
    TicketSourceType sourceType,
    Long sourceId
) {

    public static TicketIssueResponse from(Ticket ticket) {
        return new TicketIssueResponse(
            ticket.getId(),
            ticket.getTicketNumber(),
            ticket.getReservationNo(),
            ticket.getStatus(),
            ticket.getSourceType(),
            ticket.getSourceId()
        );
    }
}
