package com.t1.popcon.draw.controller;

import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.draw.dto.response.DrawAvailableDateResponse;
import com.t1.popcon.draw.dto.response.DrawOptionResponse;
import com.t1.popcon.draw.service.DrawOptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/draws")
@RequiredArgsConstructor
public class DrawOptionController {

    private final DrawOptionService drawOptionService;

    @GetMapping("/{drawId}/dates")
    public ApiResponse<List<DrawAvailableDateResponse>> getAvailableDates(@PathVariable Long drawId) {
        List<DrawAvailableDateResponse> data = drawOptionService.getAvailableDates(drawId);
        return ApiResponse.ok("선택 가능한 날짜 목록 조회를 성공했습니다.", data);
    }

    @GetMapping("/{drawId}/dates/{entryDate}/options")
    public ApiResponse<List<DrawOptionResponse>> getOptionsByDate(
        @PathVariable Long drawId,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entryDate
    ) {
        List<DrawOptionResponse> data = drawOptionService.getOptionsByDate(drawId, entryDate);
        return ApiResponse.ok("날짜별 입장 시간 목록 조회를 성공했습니다.", data);
    }
}