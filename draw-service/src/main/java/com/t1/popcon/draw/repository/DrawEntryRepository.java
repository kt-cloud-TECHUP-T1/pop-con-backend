package com.t1.popcon.draw.repository;

import com.t1.popcon.draw.domain.DrawEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DrawEntryRepository extends JpaRepository<DrawEntry, Long> {
	boolean existsByUserIdAndDrawOption_Id(Long userId, Long drawOptionId);

	List<DrawEntry> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}