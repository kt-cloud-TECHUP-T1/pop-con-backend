package com.t1.popcon.auction.bid.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import com.t1.popcon.auction.bid.domain.Bid;
import com.t1.popcon.auction.bid.domain.BidStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BidRepository extends JpaRepository<Bid, Long> {
	@Modifying
	@Query("UPDATE Bid b SET b.status = :toStatus, b.paidAt = :paidAt, b.pgTxId = :pgTxId, " +
		"b.reservationNo = :reservationNo, " +
		"b.popupTitle = :popupTitle, b.popupAddress = :popupAddress, b.thumbnailUrl = :thumbnailUrl, " +
		"b.entryDate = :entryDate, b.entryTime = :entryTime, b.startPrice = :startPrice " +
		"WHERE b.id = :id AND b.status = :fromStatus AND b.deleted = false")
	int updateStatusWithCAS(@Param("id") Long id,
		@Param("fromStatus") BidStatus fromStatus,
		@Param("toStatus") BidStatus toStatus,
		@Param("paidAt") LocalDateTime paidAt,
		@Param("pgTxId") String pgTxId,
		@Param("reservationNo") String reservationNo,
		@Param("popupTitle") String popupTitle,
		@Param("popupAddress") String popupAddress,
		@Param("thumbnailUrl") String thumbnailUrl,
		@Param("entryDate") LocalDate entryDate,
		@Param("entryTime") LocalTime entryTime,
		@Param("startPrice") Integer startPrice);

	Optional<Bid> findByReservationNo(String reservationNo);

	Optional<Bid> findByReservationNoAndUserId(String reservationNo, Long userId);

	Optional<Bid> findByIdAndUserId(Long id, Long userId);

	Optional<Bid> findByIdAndUserIdAndStatus(Long id, Long userId, BidStatus status);

	@Query("SELECT b FROM Bid b " +
		"WHERE b.userId = :userId AND b.status = :status AND b.deleted = false " +
		"ORDER BY b.createdAt DESC")
	List<Bid> findAllByUserIdAndStatusOrderByCreatedAtDesc(@Param("userId") Long userId, @Param("status") BidStatus status);
}
