package com.t1.popcon.popup.detail.controller;

import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.popup.detail.dto.InternalPopupResponse;
import com.t1.popcon.popup.detail.service.PopupDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/popups")
public class InternalPopupController {

    private final PopupDetailService popupDetailService;

    @GetMapping("/{popupId}")
    public ApiResponse<InternalPopupResponse> getPopupInternal(@PathVariable Long popupId) {
        return ApiResponse.ok(popupDetailService.getPopupInternal(popupId));
    }

    @GetMapping("/bulk")
    public ApiResponse<List<InternalPopupResponse>> getPopupsByBulkIds(@RequestParam List<Long> popupIds) {
        return ApiResponse.ok(popupDetailService.getPopupsByBulkIds(popupIds));
    }
}
