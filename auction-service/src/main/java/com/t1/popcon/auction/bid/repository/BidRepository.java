package com.t1.popcon.auction.bid.repository;

import com.t1.popcon.auction.bid.domain.Bid;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidRepository extends JpaRepository<Bid, Long> {

}