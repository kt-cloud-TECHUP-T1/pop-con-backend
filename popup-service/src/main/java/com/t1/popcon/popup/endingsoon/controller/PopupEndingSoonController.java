package com.t1.popcon.popup.endingsoon.controller;

import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.popup.dto.card.PopupCardDto;
import com.t1.popcon.popup.dto.section.PopupSectionResponse;
import com.t1.popcon.popup.endingsoon.service.PopupEndingSoonService;
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
public class PopupEndingSoonController {

    private final PopupEndingSoonService popupEndingSoonService;

    @GetMapping("/ending-soon")
    public ResponseEntity<ApiResponse<PopupSectionResponse<PopupCardDto>>> getEndingSoonPopups(
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "limit는 1 이상이어야 합니다.")
            int limit
    ) {
        PopupSectionResponse<PopupCardDto> response = popupEndingSoonService.getEndingSoonPopups(limit);
        return ResponseEntity.ok(ApiResponse.ok("곧 종료되는 팝업 조회 성공", response));
    }
}