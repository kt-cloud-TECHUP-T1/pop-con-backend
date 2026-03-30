package com.t1.popcon.ticket.dto.request;

import com.t1.popcon.ticket.domain.TicketSourceType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record TicketIssueRequest(
    @NotNull Long userId,
    @NotNull Long popupId,
    @NotNull TicketSourceType sourceType,
    @NotNull Long sourceId,
    @NotNull LocalDate entryDate,
    @NotNull LocalTime entryTime
) {
}
