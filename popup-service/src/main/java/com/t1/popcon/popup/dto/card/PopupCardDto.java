package com.t1.popcon.popup.dto.card;

import java.time.OffsetDateTime;

public record PopupCardDto(
    Long popupId,
    String title,
    String supportingText,
    String subText,
    String caption,
    String thumbnailUrl,
    Boolean liked,
    StatsDto stats,
    OverlayDto overlay,
    PhaseDto phase
) {
    public record StatsDto(
        long likeCount,
        long viewCount
    ) {
    }

    public record OverlayDto(
        OverlayType type,
        Integer rank
    ) {
    }

    public record PhaseDto(
        PhaseType type,
        PhaseStatus status,
        OffsetDateTime openAt,
        OffsetDateTime closeAt
    ) {
    }
}