package com.t1.popcon.popup.detail.service;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.popup.detail.dto.InternalPopupResponse;
import com.t1.popcon.popup.detail.dto.PopupDetailResponse;
import com.t1.popcon.popup.detail.entity.Popup;
import com.t1.popcon.popup.detail.repository.PopupRepository;
import com.t1.popcon.popup.dto.card.PhaseType;
import com.t1.popcon.popup.likes.service.PopupLikeReadService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PopupDetailService {

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    private final PopupRepository popupRepository;
    private final PopupLikeReadService popupLikeReadService;

    public PopupDetailResponse getPopupDetail(Long popupId, Long userId) {
        Popup popup = popupRepository.findById(popupId)
                .orElseThrow(() -> new CustomException(ErrorCode.POPUP_NOT_FOUND));

        boolean liked = popupLikeReadService.isLiked(popupId, userId);
        PhaseType phaseType = resolvePhaseType(popup, LocalDateTime.now(KST_ZONE));

        return PopupDetailResponse.builder()
                .phaseType(phaseType)
                .auctionId(popup.getAuctionId())
                .drawId(popup.getDrawId())
                .popupId(popup.getId())
                .liked(liked)
                .thumbnailUrl(phaseType == PhaseType.AUCTION ? popup.getHThumbUrl() : popup.getVThumbUrl())
                .title(popup.getTitle())
                .subtitle(popup.getSubtitle())
                .viewCount(popup.getViewCount())
                .likeCount(popup.getLikeCount())
                .description(popup.getDescription())
                .location(popup.getLocation())
                .reviewCount(popup.getReviewCount())
                .openAt(popup.getOpenAt())
                .closeAt(popup.getCloseAt())
                .weekdayOpen(popup.getWeekdayOpen())
                .weekdayClose(popup.getWeekdayClose())
                .weekendOpen(popup.getWeekendOpen())
                .weekendClose(popup.getWeekendClose())
                .build();
    }

    public InternalPopupResponse getPopupInternal(Long popupId) {
        Popup popup = popupRepository.findById(popupId)
                .orElseThrow(() -> new CustomException(ErrorCode.POPUP_NOT_FOUND));

        return InternalPopupResponse.builder()
                .popupId(popup.getId())
                .title(popup.getTitle())
                .location(popup.getLocation())
                .vThumbnailUrl(popup.getVThumbUrl())
                .build();
    }

    public List<InternalPopupResponse> getPopupsByBulkIds(List<Long> popupIds) {
        return popupRepository.findAllById(popupIds).stream()
                .map(popup -> InternalPopupResponse.builder()
                        .popupId(popup.getId())
                        .title(popup.getTitle())
                        .location(popup.getLocation())
                        .vThumbnailUrl(popup.getVThumbUrl())
                        .build())
                .toList();
    }

    private PhaseType resolvePhaseType(Popup popup, LocalDateTime now) {
        boolean auctionActive = popup.getAuctionId() != null
                && popup.getAuctionOpenAt() != null
                && popup.getAuctionCloseAt() != null
                && !now.isBefore(popup.getAuctionOpenAt())
                && now.isBefore(popup.getAuctionCloseAt());

        boolean drawActive = popup.getDrawId() != null
                && popup.getDrawOpenAt() != null
                && popup.getDrawCloseAt() != null
                && !now.isBefore(popup.getDrawOpenAt())
                && now.isBefore(popup.getDrawCloseAt());

        if (auctionActive) {
            return PhaseType.AUCTION;
        }

        if (drawActive) {
            return PhaseType.DRAW;
        }

        return null;
    }
}