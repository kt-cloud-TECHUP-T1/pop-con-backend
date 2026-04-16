package com.t1.popcon.auction.bid.controller;

import com.t1.popcon.auction.bid.dto.response.AuctionStatisticsResponse;
import com.t1.popcon.auction.bid.dto.response.BidHistoryResponse;
import com.t1.popcon.auction.bid.dto.response.ReservationDetailResponse;
import com.t1.popcon.auction.bid.service.BidService;
import com.t1.popcon.common.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/auctions")
@RequiredArgsConstructor
public class InternalBidController {

    private final BidService bidService;

    @GetMapping("/statistics")
    public ApiResponse<AuctionStatisticsResponse> getAuctionStatistics(@RequestParam("userId") Long userId) {
        AuctionStatisticsResponse response = bidService.getAuctionStatistics(userId);
        return ApiResponse.ok("경매 통계 조회 성공", response);
    }

    @GetMapping("/bids")
    public ApiResponse<List<BidHistoryResponse>> getBidHistory(@RequestParam("userId") Long userId) {
        List<BidHistoryResponse> responses = bidService.getBidHistory(userId);
        return ApiResponse.ok("경매 참여 이력 조회 성공", responses);
    }

    @GetMapping("/bids/{bidId}")
    public ApiResponse<ReservationDetailResponse> getBidDetail(
        @PathVariable("bidId") Long bidId,
        @RequestParam("userId") Long userId
    ) {
        ReservationDetailResponse response = bidService.getBidDetail(userId, bidId);
        return ApiResponse.ok("경매 상세 조회 성공", response);
    }
}
