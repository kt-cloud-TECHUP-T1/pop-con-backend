package com.t1.popcon.user.controller;

import com.t1.popcon.common.auth.domain.AuthUser;
import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.user.dto.history.AuctionHistoryResponse;
import com.t1.popcon.user.dto.history.DrawHistoryResponse;
import com.t1.popcon.user.dto.history.PopupLikeHistoryResponse;
import com.t1.popcon.user.dto.history.SliceResponse;
import com.t1.popcon.user.dto.history.TicketHistoryResponse;
import com.t1.popcon.user.service.UserHistoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/history")
@RequiredArgsConstructor
public class UserHistoryController {

    private final UserHistoryService userHistoryService;

    @GetMapping("/draws")
    public ApiResponse<List<DrawHistoryResponse>> getDrawHistory(@AuthenticationPrincipal AuthUser authUser) {
        List<DrawHistoryResponse> response = userHistoryService.getDrawHistory(authUser.id());
        return ApiResponse.ok("드로우 응모 내역 조회를 성공했습니다.", response);
    }

    @GetMapping("/auctions")
    public ApiResponse<List<AuctionHistoryResponse>> getAuctionHistory(@AuthenticationPrincipal AuthUser authUser) {
        List<AuctionHistoryResponse> response = userHistoryService.getAuctionHistory(authUser.id());
        return ApiResponse.ok("경매 참여 내역 조회를 성공했습니다.", response);
    }

    @GetMapping("/tickets")
    public ApiResponse<SliceResponse<TicketHistoryResponse>> getTicketHistory(
        @AuthenticationPrincipal AuthUser authUser,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        SliceResponse<TicketHistoryResponse> response = userHistoryService.getTicketHistory(authUser.id(), page, size);
        return ApiResponse.ok("티켓 목록 조회를 성공했습니다.", response);
    }

    @GetMapping("/tickets/reservations/{reservationNo}")
    public ApiResponse<TicketHistoryResponse> getTicketByReservationNo(
        @AuthenticationPrincipal AuthUser authUser,
        @PathVariable("reservationNo") String reservationNo
    ) {
        TicketHistoryResponse response = userHistoryService.getTicketByReservationNo(authUser.id(), reservationNo);
        return ApiResponse.ok("티켓 상세 조회를 성공했습니다.", response);
    }

    @GetMapping("/likes")
    public ApiResponse<SliceResponse<PopupLikeHistoryResponse>> getLikedPopups(
        @AuthenticationPrincipal AuthUser authUser,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "12") int size
    ) {
        SliceResponse<PopupLikeHistoryResponse> response = userHistoryService.getLikedPopups(authUser.id(), page, size);
        return ApiResponse.ok("찜한 팝업스토어 목록 조회를 성공했습니다.", response);
    }
}
