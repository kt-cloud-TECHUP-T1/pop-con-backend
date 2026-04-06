package com.t1.popcon.auction.repository;

import com.t1.popcon.auction.domain.Auction;
import com.t1.popcon.auction.domain.AuctionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
    List<Auction> findAllByStatusIn(List<AuctionStatus> statuses);
}
