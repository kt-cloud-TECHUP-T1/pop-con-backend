package com.t1.popcon.auction.controller;

import com.t1.popcon.auction.domain.Auction;
import com.t1.popcon.auction.domain.AuctionStatus;
import com.t1.popcon.auction.dto.response.AuctionPriceStreamResponse;
import com.t1.popcon.auction.repository.AuctionRepository;
import com.t1.popcon.auction.service.AuctionPriceService;
import com.t1.popcon.auction.service.AuctionSseService;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/auctions")
@RequiredArgsConstructor
public class AuctionSseController {

    private final AuctionRepository auctionRepository;
    private final AuctionPriceService auctionPriceService;
    private final AuctionSseService auctionSseService;

    @GetMapping(value = "/{auctionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUCTION_NOT_FOUND));

        SseEmitter emitter = auctionSseService.subscribe(auctionId);

        LocalDateTime now = LocalDateTime.now();
        AuctionStatus auctionStatus = auctionPriceService.calculateStatus(auction, now);
        int currentPrice = auctionPriceService.calculateCurrentPrice(auction, now);
        long secondsUntilNextDrop = auctionPriceService.calculateSecondsUntilNextDrop(auction, now);

        AuctionPriceStreamResponse response = AuctionPriceStreamResponse.of(
                auction,
                auctionStatus,
                currentPrice,
                secondsUntilNextDrop,
                now
        );

        try {
            emitter.send(SseEmitter.event()
                    .name("auction-price")
                    .data(response));
        } catch (IOException e) {
            emitter.complete();
        }

        return emitter;
    }
}
