package com.t1.popcon.user.client;

import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.user.client.config.FeignClientConfig;
import com.t1.popcon.user.dto.history.PopupInternalResponse;
import com.t1.popcon.user.dto.history.PopupLikeHistoryResponse;
import com.t1.popcon.user.dto.history.SliceResponse;
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

    @GetMapping("/internal/popups/likes")
    ApiResponse<SliceResponse<PopupLikeHistoryResponse>> getLikedPopups(
        @RequestParam("userId") Long userId,
        @RequestParam("page") int page,
        @RequestParam("size") int size
    );
}
