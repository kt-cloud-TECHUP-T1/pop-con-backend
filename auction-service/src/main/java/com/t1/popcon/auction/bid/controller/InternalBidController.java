package com.t1.popcon.auction.bid.controller;

import com.t1.popcon.auction.bid.dto.BidRequest;
import com.t1.popcon.auction.bid.dto.BidResponse;
import com.t1.popcon.auction.bid.dto.response.BidHistoryResponse;
import com.t1.popcon.auction.bid.service.BidService;
import com.t1.popcon.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
        return ApiResponse.ok("경매 낙찰 이력 조회에 성공했습니다.", responses);
    }

    @PostMapping("/bids/test")
    public ApiResponse<BidResponse> attemptBidForTest(
        @RequestParam("userId") Long userId,
        @Valid @RequestBody BidRequest request
    ) {
        BidResponse response = bidService.attemptBid(userId, request);
        return ApiResponse.ok("테스트 낙찰에 성공했습니다.", response);
    }
}
