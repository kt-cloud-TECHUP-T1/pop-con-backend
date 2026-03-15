package com.t1.popcon.auction.service;

import com.t1.popcon.auction.domain.Auction;
import com.t1.popcon.auction.domain.AuctionOption;
import com.t1.popcon.auction.domain.AuctionStatus;
import com.t1.popcon.auction.dto.response.AuctionAvailableDateResponse;
import com.t1.popcon.auction.dto.response.AuctionOptionResponse;
import com.t1.popcon.auction.repository.AuctionOptionRepository;
import com.t1.popcon.auction.repository.AuctionRepository;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionOptionService {

    private final AuctionRepository auctionRepository;
    private final AuctionOptionRepository auctionOptionRepository;
    private final AuctionPriceService auctionPriceService;

    // 날짜 목록 조회
    public List<AuctionAvailableDateResponse> getAvailableDates(Long auctionId) {
        Auction auction = getSelectableAuction(auctionId);

        return auctionOptionRepository
            .findByAuction_IdAndRemainingStockGreaterThanOrderByEntryDateAscEntryTimeAsc(auction.getId(), 0)
            .stream()
            .map(AuctionOption::getEntryDate)
            .distinct()
            .map(AuctionAvailableDateResponse::new)
            .toList();
    }

    // 날짜별 회차 조회
    public List<AuctionOptionResponse> getOptionsByDate(Long auctionId, LocalDate entryDate) {
        Auction auction = getSelectableAuction(auctionId);

        return auctionOptionRepository
            .findByAuction_IdAndEntryDateOrderByEntryTimeAsc(auction.getId(), entryDate)
            .stream()
            .map(option -> new AuctionOptionResponse(
                option.getId(),
                option.getEntryTime(),
                option.getRemainingStock(),
                option.isSelectable()
            ))
            .toList();
    }

    private Auction getSelectableAuction(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
            .orElseThrow(() -> new CustomException(ErrorCode.AUCTION_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        AuctionStatus currentStatus = auctionPriceService.calculateStatus(auction, now);

        if (currentStatus == AuctionStatus.CLOSED) {
            throw new CustomException(ErrorCode.AUCTION_ALREADY_CLOSED);
        }

        if (currentStatus != AuctionStatus.OPEN) {
            throw new CustomException(ErrorCode.AUCTION_NOT_OPEN);
        }

        return auction;
    }
}