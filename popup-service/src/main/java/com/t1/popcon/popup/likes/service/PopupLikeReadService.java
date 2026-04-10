package com.t1.popcon.popup.likes.service;

import com.t1.popcon.popup.likes.repository.PopupLikeRepository;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PopupLikeReadService {

    private final PopupLikeRepository popupLikeRepository;

    public boolean isLiked(Long popupId, Long userId) {
        if (popupId == null || userId == null) {
            return false;
        }
        return popupLikeRepository.existsByPopup_IdAndUserId(popupId, userId);
    }

    public Set<Long> getLikedPopupIds(Long userId, Collection<Long> popupIds) {
        if (userId == null || popupIds == null || popupIds.isEmpty()) {
            return Collections.emptySet();
        }
        return popupLikeRepository.findLikedPopupIds(userId, popupIds);
    }
}
