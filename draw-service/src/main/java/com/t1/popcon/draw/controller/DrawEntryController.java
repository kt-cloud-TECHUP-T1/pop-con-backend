package com.t1.popcon.draw.controller;

import com.t1.popcon.common.auth.domain.AuthUser;
import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.draw.dto.request.DrawEntryRequest;
import com.t1.popcon.draw.service.DrawEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/draws")
@RequiredArgsConstructor
public class DrawEntryController {

	private final DrawEntryService drawEntryService;

	@PostMapping("/{drawId}/options/{optionId}/entries")
	public ApiResponse<Void> applyForDraw(
		@PathVariable Long drawId,
		@PathVariable Long optionId,
		@Valid @RequestBody DrawEntryRequest request,
		@AuthenticationPrincipal AuthUser authUser
	) {

		drawEntryService.applyForDraw(authUser.id(), drawId, optionId, request);

		return ApiResponse.ok("드로우 응모가 완료되었습니다.");
	}
}