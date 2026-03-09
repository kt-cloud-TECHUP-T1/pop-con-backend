package com.t1.popcon.popup.banners.controller;

import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.popup.dto.card.*;
import com.t1.popcon.popup.dto.section.PopupSectionResponse;
import com.t1.popcon.popup.banners.service.PopupBannersService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/popups/banners")
public class PopupBannersController {

    private final PopupBannersService popupBannersService;

    @GetMapping
    public ResponseEntity<ApiResponse<PopupSectionResponse<PopupCardDto>>> getBanners(
        @RequestParam(defaultValue = "5") int limit
    ) {
        PopupSectionResponse<PopupCardDto> data = popupBannersService.getBanners(limit);
        return ResponseEntity.ok(ApiResponse.ok("배너 섹션 조회를 성공했습니다.", data));
    }
}