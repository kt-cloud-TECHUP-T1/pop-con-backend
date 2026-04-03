package com.t1.popcon.popup.recommended.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.popup.dto.card.PopupCardDto;
import com.t1.popcon.popup.dto.section.PopupSectionResponse;
import com.t1.popcon.popup.recommended.service.PopupRecommendedService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/popups")
@RequiredArgsConstructor
public class PopupRecommendedController {
	private final PopupRecommendedService popupRecommendedService;

	@GetMapping("/recommended")
	public ResponseEntity<ApiResponse<PopupSectionResponse<PopupCardDto>>> recommended() {
		PopupSectionResponse<PopupCardDto> response = popupRecommendedService.recommended();
		return ResponseEntity.ok(ApiResponse.ok("추천 팝업스토어 조회에 성공했습니다.", response));
	}
}
