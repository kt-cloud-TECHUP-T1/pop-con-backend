package com.t1.popcon.auction.bid.client;

import com.t1.popcon.auction.bid.client.dto.BillingKeyInternalResponse;
import com.t1.popcon.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "user-service",
    url = "${services.user-service.url:http://localhost:8081}"
)
public interface UserBillingClient {

    @GetMapping("/internal/billing/keys/default")
    ApiResponse<BillingKeyInternalResponse> getDefaultBillingKey(@RequestParam("userId") Long userId);
}
