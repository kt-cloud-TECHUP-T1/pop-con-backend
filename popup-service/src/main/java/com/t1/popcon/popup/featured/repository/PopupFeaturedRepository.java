package com.t1.popcon.popup.featured.repository;

import com.t1.popcon.popup.detail.entity.Popup;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PopupFeaturedRepository extends JpaRepository<Popup, Long> {

    @Query("""
        select p
        from Popup p
        order by (p.viewCount + (p.likeCount * :likeWeight)) desc, p.id desc
    """)
    List<Popup> findFeaturedPopups(long likeWeight, Pageable pageable);
}