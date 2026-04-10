package com.t1.popcon.popup.rankings.controller;

import com.t1.popcon.common.auth.domain.AuthUser;
import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.popup.dto.card.PopupCardDto;
import com.t1.popcon.popup.dto.section.PopupSectionResponse;
import com.t1.popcon.popup.rankings.service.PopupRankingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/popups")
@RequiredArgsConstructor
public class PopupRankingsController {

	private final PopupRankingsService popupRankingsService;

	@GetMapping("/rankings")
	public ResponseEntity<ApiResponse<PopupSectionResponse<PopupCardDto>>> getPopularRankings(
		@AuthenticationPrincipal AuthUser authUser
	) {
		PopupSectionResponse<PopupCardDto> response =
			popupRankingsService.getPopularRankings(authUser != null ? authUser.id() : null);

		return ResponseEntity.ok(ApiResponse.ok("인기 랭킹 팝업스토어 조회를 성공했습니다.", response));
	}
}
