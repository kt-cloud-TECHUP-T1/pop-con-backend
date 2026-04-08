package com.t1.popcon.popup.likes.repository;

import com.t1.popcon.popup.detail.entity.PopupLike;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PopupLikeRepository extends JpaRepository<PopupLike, Long> {

    @EntityGraph(attributePaths = "popup")
    Slice<PopupLike> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    boolean existsByPopup_IdAndUserId(Long popupId, Long userId);

    Optional<PopupLike> findByPopup_IdAndUserId(Long popupId, Long userId);
}
