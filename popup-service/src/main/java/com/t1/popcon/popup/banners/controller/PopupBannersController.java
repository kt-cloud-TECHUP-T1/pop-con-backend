package com.t1.popcon.popup.banners.controller;

import com.t1.popcon.common.auth.domain.AuthUser;
import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.popup.banners.service.PopupBannersService;
import com.t1.popcon.popup.dto.card.PopupCardDto;
import com.t1.popcon.popup.dto.section.PopupSectionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/popups/banners")
public class PopupBannersController {

    private final PopupBannersService popupBannersService;

    @GetMapping
    public ResponseEntity<ApiResponse<PopupSectionResponse<PopupCardDto>>> getBanners(
        @AuthenticationPrincipal AuthUser authUser,
        @RequestParam(defaultValue = "5") int limit
    ) {
        PopupSectionResponse<PopupCardDto> data =
                popupBannersService.getBanners(authUser != null ? authUser.id() : null, limit);
        return ResponseEntity.ok(ApiResponse.ok("배너 섹션 조회를 성공했습니다.", data));
    }
}
