package com.t1.popcon.draw.repository;

import com.t1.popcon.draw.domain.DrawEntry;
import com.t1.popcon.draw.domain.DrawEntryStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface DrawEntryRepository extends JpaRepository<DrawEntry, Long> {
	boolean existsByUserIdAndDrawOption_Id(Long userId, Long drawOptionId);

	Slice<DrawEntry> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<DrawEntry> findAllByDrawOption_IdAndStatusOrderByIdAsc(Long drawOptionId, DrawEntryStatus status);

    boolean existsByDrawOption_IdAndStatusIn(Long drawOptionId, List<DrawEntryStatus> statuses);

    boolean existsByDrawOption_IdAndStatus(Long drawOptionId, DrawEntryStatus status);

    Optional<DrawEntry> findByIdAndUserId(Long id, Long userId);
}
