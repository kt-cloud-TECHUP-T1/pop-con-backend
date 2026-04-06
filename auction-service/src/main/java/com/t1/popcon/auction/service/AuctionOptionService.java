package com.t1.popcon.auction.service;

import com.t1.popcon.auction.domain.Auction;
import com.t1.popcon.auction.domain.AuctionStatus;
import com.t1.popcon.auction.dto.response.AuctionAvailableDateResponse;
import com.t1.popcon.auction.dto.response.AuctionOptionResponse;
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
    private final AuctionPriceService auctionPriceService;
    private final AuctionStockService auctionStockService;

    public List<AuctionAvailableDateResponse> getAvailableDates(Long auctionId) {
        Auction auction = getSelectableAuction(auctionId);

        return auctionStockService.getOptionStocks(auction.getId()).values().stream()
            .filter(AuctionStockService.OptionStockSnapshot::hasStockForListing)
            .map(AuctionStockService.OptionStockSnapshot::entryDate)
            .distinct()
            .map(AuctionAvailableDateResponse::new)
            .toList();
    }

    public List<AuctionOptionResponse> getOptionsByDate(Long auctionId, LocalDate entryDate) {
        Auction auction = getSelectableAuction(auctionId);

        return auctionStockService.getOptionStocksByDate(auction.getId(), entryDate)
            .stream()
            .map(snapshot -> new AuctionOptionResponse(
                snapshot.optionId(),
                snapshot.entryTime(),
                snapshot.availableStock(),
                snapshot.pendingStock(),
                snapshot.isSelectable()
            ))
            .toList();
    }

    private Auction getSelectableAuction(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
            .orElseThrow(() -> new CustomException(ErrorCode.AUCTION_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        AuctionStatus currentStatus = auctionPriceService.calculateStatus(
            auction,
            now,
            auctionStockService.hasAvailableStock(auction.getId())
        );

        if (currentStatus == AuctionStatus.CLOSED) {
            throw new CustomException(ErrorCode.AUCTION_ALREADY_CLOSED);
        }

        if (currentStatus != AuctionStatus.OPEN) {
            throw new CustomException(ErrorCode.AUCTION_NOT_OPEN);
        }

        return auction;
    }
}
