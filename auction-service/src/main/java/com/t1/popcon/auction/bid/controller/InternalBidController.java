package com.t1.popcon.auction.bid.controller;

import com.t1.popcon.auction.bid.dto.response.BidHistoryResponse;
import com.t1.popcon.auction.bid.service.BidService;
import com.t1.popcon.common.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/auctions")
@RequiredArgsConstructor
public class InternalBidController {

    private final BidService bidService;

    @GetMapping("/bids")
    public ApiResponse<List<BidHistoryResponse>> getBidHistory(@RequestParam("userId") Long userId) {
        List<BidHistoryResponse> responses = bidService.getBidHistory(userId);
        return ApiResponse.ok("경매 응찰 이력 조회를 성공했습니다.", responses);
    }
}
