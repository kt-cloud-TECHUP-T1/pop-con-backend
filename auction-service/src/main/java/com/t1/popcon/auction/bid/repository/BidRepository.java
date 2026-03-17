package com.t1.popcon.auction.bid.repository;

import java.time.LocalDateTime;

import com.t1.popcon.auction.bid.domain.Bid;
import com.t1.popcon.auction.bid.domain.BidStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BidRepository extends JpaRepository<Bid, Long> {
	@Modifying
	@Query("UPDATE Bid b SET b.status = :toStatus, b.paidAt = :paidAt " +
		"WHERE b.id = :id AND b.status = :fromStatus AND b.deleted = false")
	int updateStatusWithCAS(@Param("id") Long id,
		@Param("fromStatus") BidStatus fromStatus,
		@Param("toStatus") BidStatus toStatus,
		@Param("paidAt") LocalDateTime paidAt);
}