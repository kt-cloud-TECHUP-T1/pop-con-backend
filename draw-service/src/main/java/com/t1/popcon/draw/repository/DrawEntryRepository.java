package com.t1.popcon.draw.repository;

import com.t1.popcon.draw.domain.DrawEntry;
import com.t1.popcon.draw.domain.DrawEntryStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface DrawEntryRepository extends JpaRepository<DrawEntry, Long> {
	boolean existsByUserIdAndDrawOption_Id(Long userId, Long drawOptionId);

	boolean existsByUserIdAndDrawId(Long userId, Long drawId);

	Slice<DrawEntry> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<DrawEntry> findAllByDrawOption_IdAndStatusOrderByIdAsc(Long drawOptionId, DrawEntryStatus status);

    boolean existsByDrawOption_IdAndStatusIn(Long drawOptionId, List<DrawEntryStatus> statuses);

    boolean existsByDrawOption_IdAndStatus(Long drawOptionId, DrawEntryStatus status);

    Optional<DrawEntry> findByIdAndUserId(Long id, Long userId);

    long countByDrawOption_Draw_Id(Long drawId);

    long countByDrawOption_Draw_IdAndStatus(Long drawId, DrawEntryStatus status);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, DrawEntryStatus status);

    @Query("SELECT COUNT(de) FROM DrawEntry de JOIN de.drawOption do JOIN do.draw d " +
           "WHERE de.userId = :userId AND de.status = :status AND d.drawCloseAt > :now")
    long countOngoingByUserId(@Param("userId") Long userId, @Param("status") DrawEntryStatus status, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(de) FROM DrawEntry de JOIN de.drawOption do JOIN do.draw d " +
           "WHERE de.userId = :userId AND de.status = :status AND d.drawCloseAt <= :now")
    long countWaitingByUserId(@Param("userId") Long userId, @Param("status") DrawEntryStatus status, @Param("now") LocalDateTime now);
  
    // 테스트 초기화용: drawId 기준 응모 내역 하드 딜리트 (소프트 딜리트된 옵션의 응모도 포함)
    @Modifying
    @Query(value = "DELETE FROM draw_entries WHERE draw_option_id IN (SELECT id FROM draw_options WHERE draw_id = :drawId)", nativeQuery = true)
    void deleteAllByDrawId(@Param("drawId") Long drawId);
}
