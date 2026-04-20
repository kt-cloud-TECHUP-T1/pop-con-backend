package com.t1.popcon.user.dto.history;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AuctionReservationDetailResponse {

    private String reservationNo;
    private String paymentMethod;
    private String cardName;
    private String cardNumber;
    private String popupTitle;
    private String popupAddress;
    private String popupThumbnail;
    private LocalDate entryDate;
    private LocalTime entryTime;
    private Integer startPrice;
    private Integer discountAmount;
    private Integer finalPrice;
    private LocalDateTime paidAt;
}
