package com.t1.popcon.popup.listings.repository;

import com.t1.popcon.popup.detail.entity.Popup;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface PopupListingsRepository extends JpaRepository<Popup, Long> {

    /**
     * 경매 목록 조회
     * - status 플래그 조합에 따라 진행 중 / 오픈 예정 / 종료 조건을 OR로 연결
     * - 오픈 일시 오름차순 정렬 (빠른 오픈 순)
     */
    @Query("""
        select p
        from Popup p
        where (:openEnabled = true
                 and p.auctionOpenAt <= :now
                 and :now < p.auctionCloseAt)
           or (:upcomingEnabled = true
                 and :now < p.auctionOpenAt)
           or (:closedEnabled = true
                 and p.auctionCloseAt <= :now)
        order by p.auctionOpenAt asc, p.id desc
    """)
    List<Popup> findAuctionPopups(
        LocalDateTime now,
        boolean openEnabled,
        boolean upcomingEnabled,
        boolean closedEnabled,
        Pageable pageable
    );

    /**
     * 드로우 목록 조회
     * - 경매 쿼리와 동일한 구조, 드로우 시간 필드 기준
     * - 드로우 오픈 예정(UPCOMING): 경매가 이미 종료된 경우에만 포함
     */
    @Query("""
        select p
        from Popup p
        where (:openEnabled = true
                 and p.drawOpenAt <= :now
                 and :now < p.drawCloseAt)
           or (:upcomingEnabled = true
                 and :now < p.drawOpenAt
                 and p.auctionCloseAt <= :now)
           or (:closedEnabled = true
                 and p.drawCloseAt <= :now)
        order by p.drawOpenAt asc, p.id desc
    """)
    List<Popup> findDrawPopups(
        LocalDateTime now,
        boolean openEnabled,
        boolean upcomingEnabled,
        boolean closedEnabled,
        Pageable pageable
    );
}
