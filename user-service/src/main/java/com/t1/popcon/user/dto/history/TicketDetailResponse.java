package com.t1.popcon.user.dto.history;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TicketDetailResponse {

    private Long ticketId;
    private Long userId;
    private Long popupId;
    private String ticketNumber;
    private String reservationNo;
    private String status;
    private String sourceType;
    private Long sourceId;
    private LocalDate entryDate;
    private LocalTime entryTime;
    private LocalDateTime issuedAt;
}
