package com.t1.popcon.user.dto.history;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DrawHistoryResponse {
    private Long id;
    private Long drawId;
    private String vThumbnailUrl;
    private String title;
    private Long price;
    private LocalDateTime paidAt;
    private String displayStatus;
    private String status;
    private LocalDateTime announcementAt;
    private boolean resultAvailable;
    private boolean resultChecked;
    private boolean clickable;
}
