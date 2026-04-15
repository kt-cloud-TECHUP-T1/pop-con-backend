// 드로우 서비스 내부 호출용 Feign 클라이언트
package com.t1.popcon.auction.bid.client;

import com.t1.popcon.auction.bid.client.config.FeignClientConfig;
import com.t1.popcon.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(
    name = "draw-service",
    url = "${services.draw-service.url:http://localhost:8084}",
    configuration = FeignClientConfig.class
)
public interface DrawServiceClient {

    // 테스트 초기화용: popupId 기준 드로우 데이터 + 대기열 Redis 초기화
    @PostMapping("/internal/admin/draws/reset/{popupId}")
    ApiResponse<Void> resetDraw(@PathVariable("popupId") Long popupId);
}
