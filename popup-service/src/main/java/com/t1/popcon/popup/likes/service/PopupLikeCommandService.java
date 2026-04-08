package com.t1.popcon.popup.likes.service;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.popup.detail.entity.Popup;
import com.t1.popcon.popup.detail.entity.PopupLike;
import com.t1.popcon.popup.detail.repository.PopupRepository;
import com.t1.popcon.popup.likes.repository.PopupLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class PopupLikeCommandService {

    private final PopupRepository popupRepository;
    private final PopupLikeRepository popupLikeRepository;

    public void likePopup(Long popupId, Long userId) {
        validateUserId(userId);

        Popup popup = popupRepository.findById(popupId)
            .orElseThrow(() -> new CustomException(ErrorCode.POPUP_NOT_FOUND));

        if (popupLikeRepository.existsByPopup_IdAndUserId(popupId, userId)) {
            return;
        }

        try {
            popupLikeRepository.save(PopupLike.create(popup, userId));
            popup.increaseLikeCount();
        } catch (DataIntegrityViolationException e) {
            // Unique constraint race: another request created the like first.
        }
    }

    public void unlikePopup(Long popupId, Long userId) {
        validateUserId(userId);

        popupRepository.findById(popupId)
            .orElseThrow(() -> new CustomException(ErrorCode.POPUP_NOT_FOUND));

        popupLikeRepository.findByPopup_IdAndUserId(popupId, userId)
            .ifPresent(popupLike -> {
                popupLikeRepository.delete(popupLike);
                popupLike.getPopup().decreaseLikeCount();
            });
    }

    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }
}
