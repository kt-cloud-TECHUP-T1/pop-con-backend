package com.t1.popcon.ticket.controller;

import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.ticket.dto.request.TicketIssueRequest;
import com.t1.popcon.ticket.dto.response.TicketDetailResponse;
import com.t1.popcon.ticket.dto.response.TicketIssueResponse;
import com.t1.popcon.ticket.service.TicketIssueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/tickets")
@RequiredArgsConstructor
public class InternalTicketController {

    private final TicketIssueService ticketIssueService;

    @PostMapping
    public ApiResponse<TicketIssueResponse> issue(@Valid @RequestBody TicketIssueRequest request) {
        TicketIssueResponse response = ticketIssueService.issue(request);
        return ApiResponse.ok("티켓 발급에 성공했습니다.", response);
    }

    @GetMapping
    public ApiResponse<Slice<TicketDetailResponse>> getTicketsByUserId(
        @RequestParam("userId") Long userId,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        Slice<TicketDetailResponse> response = ticketIssueService.getTicketsByUserId(
            userId,
            PageRequest.of(page, size)
        );
        return ApiResponse.ok("티켓 목록 조회에 성공했습니다.", response);
    }

    @GetMapping("/reservations/{reservationNo}")
    public ApiResponse<TicketDetailResponse> getTicketByReservationNo(
        @PathVariable("reservationNo") String reservationNo,
        @RequestParam("userId") Long userId
    ) {
        TicketDetailResponse response = ticketIssueService.getTicketByReservationNo(reservationNo, userId);
        return ApiResponse.ok("티켓 상세 조회에 성공했습니다.", response);
    }
}
