package com.t1.popcon.user.dto.history;

import java.time.OffsetDateTime;

public record PopupLikeHistoryResponse(
    Long popupId,
    String title,
    String supportingText,
    String subText,
    String caption,
    String thumbnailUrl,
    Boolean liked,
    Stats stats,
    Overlay overlay,
    Phase phase
) {
    public record Stats(
        long likeCount,
        long viewCount
    ) {
    }

    public record Overlay(
        String type,
        Integer rank
    ) {
    }

    public record Phase(
        String type,
        String status,
        OffsetDateTime openAt,
        OffsetDateTime closeAt
    ) {
    }
}
