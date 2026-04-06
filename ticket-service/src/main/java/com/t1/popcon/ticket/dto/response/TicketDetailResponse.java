package com.t1.popcon.ticket.dto.response;

import com.t1.popcon.ticket.domain.Ticket;
import com.t1.popcon.ticket.domain.TicketSourceType;
import com.t1.popcon.ticket.domain.TicketStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record TicketDetailResponse(
    Long ticketId,
    Long userId,
    Long popupId,
    String ticketNumber,
    String reservationNo,
    TicketStatus status,
    TicketSourceType sourceType,
    Long sourceId,
    LocalDate entryDate,
    LocalTime entryTime,
    LocalDateTime issuedAt
) {

    public static TicketDetailResponse from(Ticket ticket) {
        return new TicketDetailResponse(
            ticket.getId(),
            ticket.getUserId(),
            ticket.getPopupId(),
            ticket.getTicketNumber(),
            ticket.getReservationNo(),
            ticket.getStatus(),
            ticket.getSourceType(),
            ticket.getSourceId(),
            ticket.getEntryDate(),
            ticket.getEntryTime(),
            ticket.getIssuedAt()
        );
    }
}
