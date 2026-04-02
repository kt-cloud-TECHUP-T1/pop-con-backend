package com.t1.popcon.popup.recommended.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.t1.popcon.popup.detail.entity.Popup;

@Repository
public interface PopupRecommendedRepository extends JpaRepository<Popup, Long> {

	@Query(value = "SELECT * FROM popups WHERE close_at >= :today ORDER BY RAND() LIMIT :limit", nativeQuery = true)
	List<Popup> findRandomPopups(@Param("today") LocalDate today, @Param("limit") int limit);
}
