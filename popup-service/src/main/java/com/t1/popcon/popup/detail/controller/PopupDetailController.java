package com.t1.popcon.popup.detail.controller;

import com.t1.popcon.common.auth.domain.AuthUser;
import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.popup.detail.dto.PopupDetailResponse;
import com.t1.popcon.popup.detail.service.PopupDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/popups")
@RequiredArgsConstructor
public class PopupDetailController {

	private final PopupDetailService popupDetailService;

	@GetMapping("/{popupId}")
	public ResponseEntity<ApiResponse<PopupDetailResponse>> popupDetail(
		@PathVariable Long popupId,
		@AuthenticationPrincipal AuthUser authUser
	) {
		PopupDetailResponse response = popupDetailService.getPopupDetail(
			popupId,
			authUser != null ? authUser.id() : null
		);
		return ResponseEntity.ok(ApiResponse.ok("팝업스토어 조회를 성공했습니다.", response));
	}
}
