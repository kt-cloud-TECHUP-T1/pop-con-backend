package com.t1.popcon.popup.detail.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import com.t1.popcon.popup.detail.entity.Popup;

import java.util.Optional;

@Repository
public interface PopupRepository extends JpaRepository<Popup, Long> {

	@EntityGraph(attributePaths = "images")
	Optional<Popup> findWithImagesById(Long id);
}
