package com.t1.popcon.ticket.controller;

import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.ticket.dto.request.TicketIssueRequest;
import com.t1.popcon.ticket.dto.response.TicketIssueResponse;
import com.t1.popcon.ticket.service.TicketIssueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
