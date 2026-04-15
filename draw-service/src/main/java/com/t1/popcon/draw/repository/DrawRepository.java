package com.t1.popcon.draw.repository;

import com.t1.popcon.draw.domain.Draw;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DrawRepository extends JpaRepository<Draw, Long> {

    // popupId로 드로우 조회
    Optional<Draw> findByPopupId(Long popupId);
}