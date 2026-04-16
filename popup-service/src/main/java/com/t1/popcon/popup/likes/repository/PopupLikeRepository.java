package com.t1.popcon.popup.likes.repository;

import com.t1.popcon.popup.detail.entity.PopupLike;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PopupLikeRepository extends JpaRepository<PopupLike, Long> {

    @EntityGraph(attributePaths = "popup")
    Slice<PopupLike> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    boolean existsByPopup_IdAndUserId(Long popupId, Long userId);

    long countByUserId(Long userId);

    Optional<PopupLike> findByPopup_IdAndUserId(Long popupId, Long userId);

    @Query("""
        select pl.popup.id
        from PopupLike pl
        where pl.userId = :userId
          and pl.popup.id in :popupIds
    """)
    Set<Long> findLikedPopupIds(@Param("userId") Long userId, @Param("popupIds") Collection<Long> popupIds);
}
