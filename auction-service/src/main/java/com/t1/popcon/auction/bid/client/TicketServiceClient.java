package com.t1.popcon.auction.bid.client;

import com.t1.popcon.auction.bid.client.config.FeignClientConfig;
import com.t1.popcon.auction.bid.client.dto.TicketIssueRequest;
import com.t1.popcon.auction.bid.client.dto.TicketIssueResponse;
import com.t1.popcon.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "ticket-service",
    url = "${services.ticket-service.url:http://localhost:8086}",
    configuration = FeignClientConfig.class
)
public interface TicketServiceClient {

    @PostMapping("/internal/tickets")
    ApiResponse<TicketIssueResponse> issueTicket(@RequestBody TicketIssueRequest request);

    // 테스트 초기화용: popupId 기준 티켓 전체 삭제
    @DeleteMapping("/internal/admin/tickets/{popupId}")
    ApiResponse<Void> deleteTicketsByPopupId(@PathVariable("popupId") Long popupId);
}
