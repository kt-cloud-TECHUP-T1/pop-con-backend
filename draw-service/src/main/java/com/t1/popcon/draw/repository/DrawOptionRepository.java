package com.t1.popcon.draw.repository;

import com.t1.popcon.draw.domain.DrawOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DrawOptionRepository extends JpaRepository<DrawOption, Long> {

    List<DrawOption> findByDraw_IdOrderByEntryDateAscEntryTimeAsc(Long drawId);

    List<DrawOption> findByDraw_IdAndEntryDateOrderByEntryTimeAsc(Long drawId, LocalDate entryDate);
}