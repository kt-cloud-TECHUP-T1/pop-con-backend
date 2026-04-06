package com.t1.popcon.auction.controller;

import com.t1.popcon.auction.dto.response.AuctionDetailResponse;
import com.t1.popcon.auction.service.AuctionQueryService;
import com.t1.popcon.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auctions")
@RequiredArgsConstructor
public class AuctionQueryController {

    private final AuctionQueryService auctionQueryService;

    @GetMapping("/{auctionId}")
    public ApiResponse<AuctionDetailResponse> getAuctionDetail(@PathVariable Long auctionId) {
        return ApiResponse.ok
                ("경매 상세 조회를 성공했습니다.",
                        auctionQueryService.getAuctionDetail(auctionId));
    }
}
