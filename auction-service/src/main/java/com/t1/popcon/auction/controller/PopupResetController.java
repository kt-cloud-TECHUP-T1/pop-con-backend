// 테스트용 팝업 데이터 통합 초기화 컨트롤러
package com.t1.popcon.auction.controller;

import com.t1.popcon.auction.service.PopupResetService;
import com.t1.popcon.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auctions/internal/popups")
@RequiredArgsConstructor
public class PopupResetController {

    private final PopupResetService popupResetService;

    /**
     * 팝업 전체 데이터 초기화 (부하 테스트용)
     * - DB: bids, draw_entries 삭제 / AuctionOption.remainingStock, DrawOption.processed 원복 / Auction 상태 원복
     * - Redis: 경매 재고 키, 대기열 키 삭제 (auction + draw 양쪽)
     * - tickets: ticket-service Feign으로 삭제
     */
    @PostMapping("/{popupId}/reset")
    public ResponseEntity<ApiResponse<Void>> reset(@PathVariable Long popupId) {
        popupResetService.reset(popupId);
        popupResetService.resetRedisAndDraw(popupId);
        return ResponseEntity.ok(ApiResponse.ok("팝업 초기화 완료 (popupId: " + popupId + ")"));
    }
}
