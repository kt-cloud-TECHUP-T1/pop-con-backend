package com.t1.popcon.popup.listings.controller;

import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.popup.dto.card.PhaseStatus;
import com.t1.popcon.popup.dto.card.PhaseType;
import com.t1.popcon.popup.dto.card.PopupCardDto;
import com.t1.popcon.popup.dto.card.PopupSort;
import com.t1.popcon.popup.dto.section.PopupSectionResponse;
import com.t1.popcon.popup.listings.service.PopupListingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/popups")
public class PopupListingsController {

    private final PopupListingsService popupListingsService;

    /**
     * 경매/드로우 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PopupSectionResponse<PopupCardDto>>> getPopups(
        @RequestParam(required = false) PhaseType phaseType,
        @RequestParam(required = false) List<PhaseStatus> phaseStatus,
        @RequestParam(required = false) PopupSort sort,
        @RequestParam(defaultValue = "10") int limit
    ) {
        PopupSectionResponse<PopupCardDto> data =
            popupListingsService.getPopups(phaseType, phaseStatus, sort, limit);

        String message = popupListingsService.getMessage(phaseType, phaseStatus);

        return ResponseEntity.ok(ApiResponse.ok(message, data));
    }
}
