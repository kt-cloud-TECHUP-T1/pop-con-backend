package com.t1.popcon.popup.detail.repository;

import com.t1.popcon.popup.detail.entity.Popup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PopupRepository extends JpaRepository<Popup, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Popup popup
        set popup.likeCount = coalesce(popup.likeCount, 0) + 1
        where popup.id = :popupId
        """)
    int incrementLikeCountById(@Param("popupId") Long popupId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Popup popup
        set popup.likeCount =
            case
                when coalesce(popup.likeCount, 0) > 0 then popup.likeCount - 1
                else 0
            end
        where popup.id = :popupId
        """)
    int decrementLikeCountById(@Param("popupId") Long popupId);
}
