// 테스트용 티켓 초기화 컨트롤러 (내부 호출 전용)
package com.t1.popcon.ticket.controller;

import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.ticket.service.AdminTicketResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/admin/tickets")
@RequiredArgsConstructor
public class AdminTicketResetController {

    private final AdminTicketResetService adminTicketResetService;

    // 특정 팝업의 티켓 전체 삭제
    @DeleteMapping("/{popupId}")
    public ResponseEntity<ApiResponse<Void>> deleteByPopupId(@PathVariable Long popupId) {
        adminTicketResetService.deleteByPopupId(popupId);
        return ResponseEntity.ok(ApiResponse.ok("티켓 초기화 완료 (popupId: " + popupId + ")"));
    }
}
