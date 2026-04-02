package com.t1.popcon.popup.rankings.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.t1.popcon.popup.detail.entity.Popup;

@Repository
public interface PopupRankingsRepository extends JpaRepository<Popup, Long> {

	/**
	 * 1. 가중치 점수 합계 (DESC)
	 * 2. 리뷰 수 (DESC) - 가중치 20점
	 * 3. 좋아요 수 (DESC) - 가중치 10점
	 * 4. 조회수 (DESC) - 가중치 1점
	 * 5. ID (DESC) - 최종 동률 시 최신순
	 */
	@Query("SELECT p FROM Popup p WHERE p.closeAt >= :today " +
		"ORDER BY (p.viewCount * 1 + p.likeCount * 10 + p.reviewCount * 20) DESC, " +
		"p.reviewCount DESC, p.likeCount DESC, p.viewCount DESC, p.id DESC")
	List<Popup> findTop10ByOrderByWeightedScore(@Param("today") LocalDate today, Pageable pageable);
}
