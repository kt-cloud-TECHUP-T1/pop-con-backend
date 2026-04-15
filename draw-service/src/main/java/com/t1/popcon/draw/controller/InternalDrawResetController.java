// 테스트용 드로우 초기화 컨트롤러 (내부 호출 전용)
package com.t1.popcon.draw.controller;

import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.draw.service.InternalDrawResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/admin/draws")
@RequiredArgsConstructor
public class InternalDrawResetController {

    private final InternalDrawResetService internalDrawResetService;

    // popupId 기준 드로우 데이터 + 대기열 Redis 초기화
    @PostMapping("/reset/{popupId}")
    public ResponseEntity<ApiResponse<Void>> reset(@PathVariable Long popupId) {
        internalDrawResetService.resetByPopupId(popupId);
        internalDrawResetService.resetQueueByPopupId(popupId);
        return ResponseEntity.ok(ApiResponse.ok("드로우 초기화 완료 (popupId: " + popupId + ")"));
    }
}
