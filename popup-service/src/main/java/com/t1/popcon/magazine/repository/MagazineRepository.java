package com.t1.popcon.magazine.repository;

import com.t1.popcon.magazine.entity.Magazine;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MagazineRepository extends JpaRepository<Magazine, Long> {

    List<Magazine> findByDeletedFalseOrderByPublishedAtDesc(Pageable pageable);
}
