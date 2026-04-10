package com.t1.popcon.auction.bid.client.dto;

import lombok.Builder;

@Builder
public record PopupInternalResponse(
	Long popupId,
	String title,
	String location,
	String hThumbnailUrl,
	String vThumbnailUrl
) {
}
