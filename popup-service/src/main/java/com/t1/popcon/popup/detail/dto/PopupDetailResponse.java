package com.t1.popcon.popup.detail.dto;

import com.t1.popcon.popup.dto.card.PhaseType;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;

@Builder
public record PopupDetailResponse(
    PhaseType phaseType,
    Long auctionId,
    Long drawId,
    Long popupId,
    Boolean liked,
    String thumbnailUrl,
    String title,
    String subtitle,
    Long viewCount,
    Long likeCount,
    String description,
    String location,
    Long reviewCount,
    LocalDate openAt,
    LocalDate closeAt,
    LocalTime weekdayOpen,
    LocalTime weekdayClose,
    LocalTime weekendOpen,
    LocalTime weekendClose
) {
    public PopupDetailResponse {
        if (liked == null) {
            liked = false;
        }
    }
}
