package com.t1.popcon.popup.detail.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record InternalPopupResponse(
    Long popupId,
    String title,
    String hThumbnailUrl,
    String vThumbnailUrl,
    String address,

    @JsonProperty("thumbnailUrl")
    String thumbnailUrl,

    @JsonProperty("location")
    String location
) {
}
