package com.t1.popcon.popup.detail.controller;

import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.popup.detail.dto.InternalPopupResponse;
import com.t1.popcon.popup.detail.service.PopupDetailService;
import com.t1.popcon.popup.dto.card.PopupCardDto;
import com.t1.popcon.popup.likes.dto.SliceResponse;
import com.t1.popcon.popup.likes.service.PopupLikeQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/popups")
public class InternalPopupController {

    private final PopupDetailService popupDetailService;
    private final PopupLikeQueryService popupLikeQueryService;

    @GetMapping("/{popupId}")
    public ApiResponse<InternalPopupResponse> getPopupInternal(@PathVariable Long popupId) {
        return ApiResponse.ok(popupDetailService.getPopupInternal(popupId));
    }

    @GetMapping("/bulk")
    public ApiResponse<List<InternalPopupResponse>> getPopupsByBulkIds(@RequestParam List<Long> popupIds) {
        return ApiResponse.ok(popupDetailService.getPopupsByBulkIds(popupIds));
    }

    @GetMapping("/likes")
    public ApiResponse<SliceResponse<PopupCardDto>> getLikedPopups(
        @RequestParam Long userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size
    ) {
        return ApiResponse.ok(popupLikeQueryService.getLikedPopups(userId, page, size));
    }
}
