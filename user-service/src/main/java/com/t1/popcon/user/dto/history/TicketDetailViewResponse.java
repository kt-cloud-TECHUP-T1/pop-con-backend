package com.t1.popcon.user.dto.history;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketDetailViewResponse {

    private Long ticketId;
    private String ticketNumber;
    private String reservationNo;
    private String status;
    private String displayStatus;
    private String sourceType;
    private Long sourceId;
    private Long popupId;
    private String popupTitle;
    private String popupAddress;
    private String thumbnailUrl;
    private LocalDate entryDate;
    private LocalTime entryTime;
    private LocalDateTime issuedAt;
    private String qrValue;
    private String userName;
    private String userPhoneNumber;
    private String userEmail;
    private String paymentMethod;
    private String cardName;
    private String cardNumber;
    private LocalDateTime paidAt;
    private Integer originalPrice;
    private Integer discountAmount;
    private Integer finalPrice;
}
