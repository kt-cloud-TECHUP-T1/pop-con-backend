package com.t1.popcon.draw.dto.response;

import com.t1.popcon.draw.client.dto.TicketIssueResponse;
import com.t1.popcon.draw.domain.DrawEntryStatus;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record DrawResultConfirmResponse(
    Long drawEntryId,
    Long drawId,
    String status,
    boolean winner,
    LocalDateTime announcementAt,
    LocalDateTime resultCheckedAt,
    Integer topPercent,
    TicketIssueResponse ticket
) {
    public static DrawResultConfirmResponse of(
        Long drawEntryId,
        Long drawId,
        DrawEntryStatus status,
        LocalDateTime announcementAt,
        LocalDateTime resultCheckedAt,
        Integer topPercent,
        TicketIssueResponse ticket
    ) {
        return DrawResultConfirmResponse.builder()
            .drawEntryId(drawEntryId)
            .drawId(drawId)
            .status(status.name())
            .winner(status == DrawEntryStatus.WINNER)
            .announcementAt(announcementAt)
            .resultCheckedAt(resultCheckedAt)
            .topPercent(topPercent)
            .ticket(ticket)
            .build();
    }
}
