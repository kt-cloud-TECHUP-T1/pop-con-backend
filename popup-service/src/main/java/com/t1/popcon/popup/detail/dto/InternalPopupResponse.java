package com.t1.popcon.popup.detail.dto;

import lombok.Builder;

@Builder
public record InternalPopupResponse(
    Long popupId,
    String title,
    String thumbnailUrl,
    String location
) {
}
