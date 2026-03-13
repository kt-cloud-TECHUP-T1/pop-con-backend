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
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionOptionService {

    private final AuctionRepository auctionRepository;
    private final AuctionOptionRepository auctionOptionRepository;

    public List<AuctionAvailableDateResponse> getAvailableDates(Long auctionId) {
        Auction auction = getAuctionForSelection(auctionId);

        return auctionOptionRepository.findByAuction_IdOrderByAuctionDateAscEntryTimeAsc(auction.getId())
            .stream()
            .filter(AuctionOption::isSelectable)   // 남은 재고 있는 날짜만 노출
            .map(AuctionOption::getAuctionDate)
            .distinct()                            // 같은 날짜 중복 제거
            .map(AuctionAvailableDateResponse::new)
            .toList();
    }

    public List<AuctionOptionResponse> getOptionsByDate(Long auctionId, LocalDate auctionDate) {
        Auction auction = getAuctionForSelection(auctionId);

        return auctionOptionRepository.findByAuction_IdAndAuctionDateOrderByEntryTimeAsc(auction.getId(), auctionDate)
            .stream()
            .map(option -> new AuctionOptionResponse(
                option.getId(),
                option.getEntryTime(),
                option.getRemainingStock(),
                option.isSelectable()
            ))
            .toList();
    }

    private Auction getAuctionForSelection(Long auctionId) {
        // 회차 선택은 오픈 전/종료 후에는 막고 싶으면 OPEN만 허용
        return auctionRepository.findByIdAndStatusIn(auctionId, List.of(AuctionStatus.OPEN))
            .orElseThrow(() -> new CustomException(ErrorCode.AUCTION_NOT_OPEN));
    }
}