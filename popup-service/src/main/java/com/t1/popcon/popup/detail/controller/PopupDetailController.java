package com.t1.popcon.popup.detail.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.popup.detail.dto.PopupDetailResponse;
import com.t1.popcon.popup.detail.service.PopupDetailService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/popups")
@RequiredArgsConstructor
public class PopupDetailController {

	private final PopupDetailService popupDetailService;

	@GetMapping("/{popupId}")
	public ResponseEntity<ApiResponse<PopupDetailResponse>> popupDetail(@PathVariable Long popupId) {
		PopupDetailResponse response = popupDetailService.getPopupDetail(popupId);
		return ResponseEntity.ok(ApiResponse.ok("팝업스토어 조회를 성공했습니다.", response));
	}
}
