package com.t1.popcon.user.dto.history;

public record PopupInternalResponse(
    Long popupId,
    String title,
    String thumbnailUrl,
    String location
) {
}
