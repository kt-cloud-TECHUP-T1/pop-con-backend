package com.t1.popcon.user.client;

import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.user.client.config.FeignClientConfig;
import com.t1.popcon.user.dto.history.DrawHistoryResponse;
import com.t1.popcon.user.dto.history.SliceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "draw-service",
    url = "${services.draw-service.url:http://localhost:8083}",
    configuration = FeignClientConfig.class
)
public interface DrawServiceClient {

    @GetMapping("/internal/draws/entries")
    ApiResponse<SliceResponse<DrawHistoryResponse>> getDrawEntries(
        @RequestParam("userId") Long userId,
        @RequestParam("page") int page,
        @RequestParam("size") int size
    );
}
