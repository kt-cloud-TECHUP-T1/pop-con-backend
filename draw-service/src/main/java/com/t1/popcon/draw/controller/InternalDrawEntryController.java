package com.t1.popcon.draw.controller;

import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.draw.dto.response.DrawEntryDetailResponse;
import com.t1.popcon.draw.dto.response.DrawEntryResponse;
import com.t1.popcon.draw.dto.response.DrawExecuteResponse;
import com.t1.popcon.draw.service.DrawEntryService;
import com.t1.popcon.draw.service.DrawResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/draws")
@RequiredArgsConstructor
public class InternalDrawEntryController {

    private final DrawEntryService drawEntryService;
    private final DrawResultService drawResultService;

    @GetMapping("/entries")
    public ApiResponse<Slice<DrawEntryResponse>> getEntriesByUserId(
        @RequestParam("userId") Long userId,
        Pageable pageable
    ) {
        Slice<DrawEntryResponse> responses = drawEntryService.getEntriesByUserId(userId, pageable);
        return ApiResponse.ok("응모 내역 조회 성공", responses);
    }

    @GetMapping("/entries/{entryId}")
    public ApiResponse<DrawEntryDetailResponse> getEntryDetail(
        @PathVariable("entryId") Long entryId,
        @RequestParam("userId") Long userId
    ) {
        DrawEntryDetailResponse response = drawEntryService.getEntryDetail(userId, entryId);
        return ApiResponse.ok("응모 상세 조회 성공", response);
    }

    @PostMapping("/options/{optionId}/execute")
    public ApiResponse<DrawExecuteResponse> executeDraw(@PathVariable("optionId") Long optionId) {
        DrawExecuteResponse response = drawResultService.executeDraw(optionId);
        return ApiResponse.ok("추첨 실행 성공", response);
    }
}
