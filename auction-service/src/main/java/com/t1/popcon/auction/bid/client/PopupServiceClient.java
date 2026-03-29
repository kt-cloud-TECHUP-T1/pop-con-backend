package com.t1.popcon.auction.bid.client;

import com.t1.popcon.auction.bid.client.config.FeignClientConfig;
import com.t1.popcon.auction.bid.client.dto.PopupInternalResponse;
import com.t1.popcon.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
	name = "popup-service",
	url = "${services.popup-service.url:http://localhost:8082}",
	configuration =  FeignClientConfig.class
)
public interface PopupServiceClient {

	@GetMapping("/internal/popups/{popupId}")
	ApiResponse<PopupInternalResponse> getPopupDetail(@PathVariable("popupId") Long popupId);
}
