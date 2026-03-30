package com.t1.popcon.auction.bid.client;

import com.t1.popcon.auction.bid.client.config.FeignClientConfig;
import com.t1.popcon.auction.bid.client.dto.TicketIssueRequest;
import com.t1.popcon.auction.bid.client.dto.TicketIssueResponse;
import com.t1.popcon.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "ticket-service",
    url = "${services.ticket-service.url:http://localhost:8085}",
    configuration = FeignClientConfig.class
)
public interface TicketServiceClient {

    @PostMapping("/internal/tickets")
    ApiResponse<TicketIssueResponse> issueTicket(@RequestBody TicketIssueRequest request);
}
