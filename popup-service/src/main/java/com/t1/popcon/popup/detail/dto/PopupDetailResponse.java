package com.t1.popcon.popup.detail.dto;

import com.t1.popcon.popup.dto.card.PhaseType;
import lombok.Builder;

import java.time.LocalTime;
import java.util.List;

@Builder
public record PopupDetailResponse(
    PhaseType phaseType,
    Long auctionId,
    Long drawId,
    Long popupId,
    Boolean liked,
    String thumbnailUrl,
    List<ImageResponse> images,
    String title,
    String subtitle,
    Long viewCount,
    Long likeCount,
    String description,
    String location,
    Long reviewCount,
    String openAt,
    String closeAt,
    LocalTime weekdayOpen,
    LocalTime weekdayClose,
    LocalTime weekendOpen,
    LocalTime weekendClose
) {
    public PopupDetailResponse {
        if (images == null) {
            images = List.of();
        }
        if (liked == null) {
            liked = false;
        }
    }

    public record ImageResponse(
        Long id,
        String imageUrl,
        Integer sortOrder
    ) {
    }
}
