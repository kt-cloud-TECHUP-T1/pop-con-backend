package com.t1.popcon.draw.repository;

import com.t1.popcon.draw.domain.DrawEntry;
import com.t1.popcon.draw.domain.DrawEntryStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DrawEntryRepository extends JpaRepository<DrawEntry, Long> {
	boolean existsByUserIdAndDrawOption_Id(Long userId, Long drawOptionId);

	Slice<DrawEntry> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<DrawEntry> findAllByDrawOption_IdAndStatusOrderByIdAsc(Long drawOptionId, DrawEntryStatus status);

    boolean existsByDrawOption_IdAndStatusIn(Long drawOptionId, List<DrawEntryStatus> statuses);

    boolean existsByDrawOption_IdAndStatus(Long drawOptionId, DrawEntryStatus status);

    Optional<DrawEntry> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, DrawEntryStatus status);

    @Query("SELECT COUNT(de) FROM DrawEntry de JOIN de.drawOption do JOIN do.draw d " +
           "WHERE de.userId = :userId AND de.status = 'APPLIED' AND d.drawCloseAt > :now")
    long countOngoingByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(de) FROM DrawEntry de JOIN de.drawOption do JOIN do.draw d " +
           "WHERE de.userId = :userId AND de.status = 'APPLIED' AND d.drawCloseAt <= :now")
    long countWaitingByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
