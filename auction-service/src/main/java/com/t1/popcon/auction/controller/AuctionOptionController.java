package com.t1.popcon.auction.controller;

import com.t1.popcon.auction.dto.response.AuctionAvailableDateResponse;
import com.t1.popcon.auction.dto.response.AuctionOptionResponse;
import com.t1.popcon.auction.service.AuctionOptionService;
import com.t1.popcon.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/auctions")
@RequiredArgsConstructor
public class AuctionOptionController {

    private final AuctionOptionService auctionOptionService;

    @GetMapping("/{auctionId}/dates")
    public ApiResponse<List<AuctionAvailableDateResponse>> getAvailableDates(@PathVariable Long auctionId) {
        List<AuctionAvailableDateResponse> data = auctionOptionService.getAvailableDates(auctionId);
        return ApiResponse.ok("선택 가능한 날짜 목록 조회를 성공했습니다.", data);
    }

    @GetMapping("/{auctionId}/dates/{entryDate}/options")
    public ApiResponse<List<AuctionOptionResponse>> getOptionsByDate(
        @PathVariable Long auctionId,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entryDate
    ) {
        List<AuctionOptionResponse> data = auctionOptionService.getOptionsByDate(auctionId, entryDate);
        return ApiResponse.ok("날짜별 입장 시간 목록 조회를 성공했습니다.", data);
    }
}