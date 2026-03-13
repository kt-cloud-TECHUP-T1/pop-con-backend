package com.t1.popcon.auction.repository;

import com.t1.popcon.auction.domain.AuctionOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AuctionOptionRepository extends JpaRepository<AuctionOption, Long> {

    List<AuctionOption> findByAuction_IdAndDeletedFalseOrderByAuctionDateAscEntryTimeAsc(Long auctionId);

    List<AuctionOption> findByAuction_IdAndAuctionDateAndDeletedFalseOrderByEntryTimeAsc(
        Long auctionId,
        LocalDate auctionDate
    );

    Optional<AuctionOption> findByIdAndDeletedFalse(Long optionId);
}