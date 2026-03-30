package com.t1.popcon.user.client;

import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.user.client.config.FeignClientConfig;
import com.t1.popcon.user.dto.history.AuctionHistoryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
	name = "auction-service",
	url = "${services.auction-service.url:http://localhost:8084}",
	configuration = FeignClientConfig.class
)
public interface AuctionServiceClient {

	@GetMapping("/internal/auctions/bids")
	ApiResponse<List<AuctionHistoryResponse>> getAuctionBids(@RequestParam("userId") Long userId);
}
