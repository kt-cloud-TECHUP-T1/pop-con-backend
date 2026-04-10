package com.t1.popcon.user.client;

import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.user.client.config.FeignClientConfig;
import com.t1.popcon.user.dto.history.SliceResponse;
import com.t1.popcon.user.dto.history.TicketHistoryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "ticket-service",
    url = "${services.ticket-service.url:http://localhost:8086}",
    configuration = FeignClientConfig.class
)
public interface TicketServiceClient {

    @GetMapping("/internal/tickets")
    ApiResponse<SliceResponse<TicketHistoryResponse>> getTickets(
        @RequestParam("userId") Long userId,
        @RequestParam("page") int page,
        @RequestParam("size") int size
    );

    @GetMapping("/internal/tickets/reservations/{reservationNo}")
    ApiResponse<TicketHistoryResponse> getTicketByReservationNo(
        @PathVariable("reservationNo") String reservationNo,
        @RequestParam("userId") Long userId
    );

    @GetMapping("/internal/tickets/{ticketId}")
    ApiResponse<TicketHistoryResponse> getTicketById(
        @PathVariable("ticketId") Long ticketId,
        @RequestParam("userId") Long userId
    );
}
