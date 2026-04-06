package com.t1.popcon.popup.endingsoon.repository;

import com.t1.popcon.popup.detail.entity.Popup;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface PopupEndingSoonRepository extends JpaRepository<Popup, Long> {

    @Query("""
        select p
        from Popup p
        where p.closeAt >= :now
          and p.closeAt <= :deadline
        order by p.closeAt asc, p.id desc
    """)
    List<Popup> findEndingSoonPopups(LocalDate now, LocalDate deadline, Pageable pageable);
}
