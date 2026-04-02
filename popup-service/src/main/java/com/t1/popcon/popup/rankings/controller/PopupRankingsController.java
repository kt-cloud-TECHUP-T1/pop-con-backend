package com.t1.popcon.popup.rankings.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.popup.dto.card.PopupCardDto;
import com.t1.popcon.popup.dto.section.PopupSectionResponse;
import com.t1.popcon.popup.rankings.service.PopupRankingsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/popups")
@RequiredArgsConstructor
public class PopupRankingsController {

	private final PopupRankingsService popupRankingsService;

	@GetMapping("/rankings")
	public ResponseEntity<ApiResponse<PopupSectionResponse<PopupCardDto>>> getPopularRankings() {
		PopupSectionResponse<PopupCardDto> response = popupRankingsService.getPopularRankings();

		return ResponseEntity.ok(ApiResponse.ok("인기 랭킹 팝업스토어 조회에 성공했습니다.", response));
	}
}
