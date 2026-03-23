package com.t1.popcon.draw.controller;

import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.draw.service.DrawEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/draws")
@RequiredArgsConstructor
public class DrawEntryController {

	private final DrawEntryService drawEntryService;

	@PostMapping("/{drawId}/options/{optionId}/entries")
	public ApiResponse<Void> applyForDraw(
		@PathVariable Long drawId,
		@PathVariable Long optionId
		// TODO: 추후 소셜 로그인 연동 시, 토큰에서 유저 정보를 가져오는 어노테이션 추가 필요
		// 예: @AuthenticationPrincipal Long userId
	) {
		Long currentUserId = 1L;

		drawEntryService.applyForDraw(currentUserId, optionId);

		return ApiResponse.ok("드로우 응모가 완료되었습니다.");
	}
}