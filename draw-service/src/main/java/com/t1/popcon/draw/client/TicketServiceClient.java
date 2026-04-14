package com.t1.popcon.draw.client;

import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.draw.client.config.FeignClientConfig;
import com.t1.popcon.draw.client.dto.TicketIssueRequest;
import com.t1.popcon.draw.client.dto.TicketIssueResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "ticket-service",
    url = "${services.ticket-service.url}",
    configuration = FeignClientConfig.class
)
public interface TicketServiceClient {

    @PostMapping("/internal/tickets")
    ApiResponse<TicketIssueResponse> issueTicket(@RequestBody TicketIssueRequest request);
}
