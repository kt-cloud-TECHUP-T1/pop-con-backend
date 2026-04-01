package com.t1.popcon.draw.client.dto;

import lombok.Builder;

@Builder
public record PopupInternalResponse(
	Long popupId,
	String title,
	String hThumbnailUrl,
	String vThumbnailUrl,
	String location
) {
}