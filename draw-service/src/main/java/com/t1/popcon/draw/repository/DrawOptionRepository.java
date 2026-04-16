package com.t1.popcon.draw.repository;

import com.t1.popcon.draw.domain.DrawOption;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DrawOptionRepository extends JpaRepository<DrawOption, Long> {

    List<DrawOption> findByDraw_IdOrderByEntryDateAscEntryTimeAsc(Long drawId);

    List<DrawOption> findByDraw_IdAndEntryDateOrderByEntryTimeAsc(Long drawId, LocalDate entryDate);

    List<DrawOption> findDistinctByDraw_DrawCloseAtBeforeAndProcessedFalse(LocalDateTime drawCloseAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select drawOption from DrawOption drawOption join fetch drawOption.draw where drawOption.id = :drawOptionId")
    Optional<DrawOption> findByIdForUpdate(@Param("drawOptionId") Long drawOptionId);

    // 테스트 초기화용: drawId 기준 processed 원복
    @Modifying
    @Query("UPDATE DrawOption o SET o.processed = false, o.processedAt = null WHERE o.draw.id = :drawId AND o.deleted = false")
    int resetProcessedByDrawId(@Param("drawId") Long drawId);
}
