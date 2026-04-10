package com.t1.popcon.user.dto.history;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DrawEntryDetailResponse {

    private Long drawEntryId;
    private Long drawId;
    private String popupTitle;
    private String popupAddress;
    private String popupThumbnail;
    private LocalDate entryDate;
    private LocalTime entryTime;
    private String userName;
    private String userPhoneNumber;
    private LocalDateTime paidAt;
    private String status;
    private LocalDateTime ticketIssuedAt;
}
