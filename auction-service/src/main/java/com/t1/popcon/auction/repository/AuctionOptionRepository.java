package com.t1.popcon.auction.repository;

import com.t1.popcon.auction.domain.AuctionOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AuctionOptionRepository extends JpaRepository<AuctionOption, Long> {

    List<AuctionOption> findByAuction_IdOrderByEntryDateAscEntryTimeAsc(Long auctionId);

    List<AuctionOption> findByAuction_IdAndEntryDateOrderByEntryTimeAsc(Long auctionId, LocalDate entryDate);

    Optional<AuctionOption> findById(Long optionId);
}