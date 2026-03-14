package com.t1.popcon.auction.repository;

import com.t1.popcon.auction.domain.AuctionOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AuctionOptionRepository extends JpaRepository<AuctionOption, Long> {

    List<AuctionOption> findByAuction_IdAndRemainingStockGreaterThanOrderByEntryDateAscEntryTimeAsc(
        Long auctionId,
        Integer remainingStock
    );

    List<AuctionOption> findByAuction_IdAndEntryDateOrderByEntryTimeAsc(
        Long auctionId,
        LocalDate entryDate
    );
}