package com.t1.popcon.draw.dto.response;

import com.t1.popcon.draw.client.dto.TicketIssueResponse;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record DrawResultConfirmResponse(
    Long drawEntryId,
    Long drawId,
    LocalDateTime announcementAt,
    LocalDateTime resultCheckedAt,
    TicketIssueResponse ticket
) {
}
