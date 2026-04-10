package com.t1.popcon.user.dto.history;

public record TicketPurchaserProfileResponse(
    Long userId,
    String userName,
    String userPhoneNumber,
    String userEmail
) {
}
