package com.t1.popcon.auction.repository;

import com.t1.popcon.auction.domain.AuctionOption;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AuctionOptionRepository extends JpaRepository<AuctionOption, Long> {

    List<AuctionOption> findByAuction_IdOrderByEntryDateAscEntryTimeAsc(Long auctionId);

    List<AuctionOption> findByAuction_IdAndRemainingStockGreaterThanOrderByEntryDateAscEntryTimeAsc(
        Long auctionId,
        Integer remainingStock
    );

    List<AuctionOption> findByAuction_IdAndEntryDateOrderByEntryTimeAsc(
        Long auctionId,
        LocalDate entryDate
    );

    @EntityGraph(attributePaths = "auction")
    @Query("SELECT ao FROM AuctionOption ao WHERE ao.id = :id")
    Optional<AuctionOption> findByIdWithAuction(@Param("id") Long id);

    @Modifying
    @Query("UPDATE AuctionOption ao SET ao.remainingStock = ao.remainingStock - 1, ao.version = ao.version + 1 WHERE ao.id = :id AND ao.remainingStock > 0")
    int decreaseStockAtomic(@Param("id") Long id);
}
