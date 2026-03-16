package com.t1.popcon.draw.repository;

import com.t1.popcon.draw.domain.Draw;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DrawRepository extends JpaRepository<Draw, Long> {

    Optional<Draw> findByIdAndDeletedFalse(Long drawId);
}