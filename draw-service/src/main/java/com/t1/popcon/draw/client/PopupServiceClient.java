package com.t1.popcon.draw.client;

import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.draw.client.config.FeignClientConfig;
import com.t1.popcon.draw.client.dto.PopupInternalResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
	name = "popup-service",
	url = "${services.popup-service.url:http://localhost:8082}",
	configuration = FeignClientConfig.class
)
public interface PopupServiceClient {

	@GetMapping("/internal/popups/{popupId}")
	ApiResponse<PopupInternalResponse> getPopupDetail(@PathVariable("popupId") Long popupId);

	@GetMapping("/internal/popups/bulk")
	ApiResponse<List<PopupInternalResponse>> getPopupsByBulkIds(@RequestParam("popupIds") List<Long> popupIds);
}