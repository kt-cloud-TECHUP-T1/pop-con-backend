package com.t1.popcon.user.controller;

import com.t1.popcon.common.auth.domain.AuthUser;
import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.user.dto.history.AuctionHistoryResponse;
import com.t1.popcon.user.dto.history.DrawHistoryResponse;
import com.t1.popcon.user.service.UserHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
		return ApiResponse.ok("경매 낙찰 내역 조회를 성공했습니다.", response);
	}
}
