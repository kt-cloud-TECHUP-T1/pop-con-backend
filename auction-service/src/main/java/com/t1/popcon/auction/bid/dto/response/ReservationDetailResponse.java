package com.t1.popcon.auction.bid.dto.response;

import lombok.Builder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Builder
public record ReservationDetailResponse(
    String reservationNo,
    String popupTitle,
    String popupAddress,
    String popupThumbnail,
    LocalDate entryDate,
    LocalTime entryTime,
    Integer startPrice,
    Integer discountAmount,
    Integer finalPrice,
    LocalDateTime paidAt
) {
}
