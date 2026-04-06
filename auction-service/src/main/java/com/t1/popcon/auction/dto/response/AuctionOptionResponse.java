package com.t1.popcon.auction.dto.response;

import java.time.LocalTime;

public record AuctionOptionResponse(
    Long optionId,
    LocalTime entryTime,
    int remainingStock,
    int pendingStock,
    boolean selectable
) {
}
