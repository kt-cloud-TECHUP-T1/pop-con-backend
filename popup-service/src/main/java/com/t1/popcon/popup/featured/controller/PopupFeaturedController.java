package com.t1.popcon.popup.featured.controller;

import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.popup.dto.card.PopupCardDto;
import com.t1.popcon.popup.dto.section.PopupSectionResponse;
import com.t1.popcon.popup.featured.service.PopupFeaturedService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/popups")
@RequiredArgsConstructor
@Validated
public class PopupFeaturedController {

    private final PopupFeaturedService popupFeaturedService;

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<PopupSectionResponse<PopupCardDto>>> getFeaturedPopups(
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "limit는 1 이상이어야 합니다.")
            int limit
    ) {
        PopupSectionResponse<PopupCardDto> response = popupFeaturedService.getFeaturedPopups(limit);
        return ResponseEntity.ok(ApiResponse.ok("주목할만한 팝업 조회 성공", response));
    }
}