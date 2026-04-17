package com.t1.popcon.auction.bid.client.dto;

// 티켓 서비스로부터 받는 티켓 상세 응답 (ticketId 포함)
public record TicketDetailResponse(
    Long ticketId,
    String reservationNo,
    String status
) {
}
