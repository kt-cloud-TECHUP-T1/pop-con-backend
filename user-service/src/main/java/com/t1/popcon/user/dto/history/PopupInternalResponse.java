package com.t1.popcon.user.dto.history;

public record PopupInternalResponse(
    Long popupId,
    String title,
    String hThumbnailUrl,
    String vThumbnailUrl,
    String location
) {
}
