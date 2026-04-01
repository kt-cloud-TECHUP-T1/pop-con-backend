package com.t1.popcon.popup.detail.service;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.popup.detail.dto.InternalPopupResponse;
import com.t1.popcon.popup.detail.dto.PopupDetailResponse;
import com.t1.popcon.popup.detail.entity.Popup;
import com.t1.popcon.popup.detail.repository.PopupRepository;
import com.t1.popcon.popup.dto.card.PhaseType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PopupDetailService {

    private final PopupRepository popupRepository;

    public PopupDetailResponse getPopupDetail(Long popupId) {
        Popup popup = popupRepository.findWithImagesById(popupId)
                .orElseThrow(() -> new CustomException(ErrorCode.POPUP_NOT_FOUND));

        PhaseType phaseType = resolvePhaseType(popup, LocalDateTime.now());

        return PopupDetailResponse.builder()
                .phaseType(phaseType)
                .auctionId(popup.getAuctionId())
                .drawId(popup.getDrawId())
                .popupId(popup.getId())
                // TODO: popup_like 서비스/레포지토리 구현 후 현재 사용자 기준 좋아요 여부로 교체 필요
                .liked(false)
                .thumbnailUrl(popup.getThumbnailUrl())
                .images(popup.getImages().stream()
                        .map(image -> new PopupDetailResponse.ImageResponse(
                                image.getId(),
                                image.getImageUrl(),
                                image.getSortOrder()
                        ))
                        .toList())
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
                .address(popup.getLocation())
                .thumbnailUrl(popup.getThumbnailUrl())
                .hThumbnailUrl(popup.getThumbnailUrl())
                .vThumbnailUrl(popup.getThumbnailUrl())
                .build();
    }

    public List<InternalPopupResponse> getPopupsByBulkIds(List<Long> popupIds) {
        return popupRepository.findAllById(popupIds).stream()
                .map(popup -> InternalPopupResponse.builder()
                        .popupId(popup.getId())
                        .title(popup.getTitle())
                        .location(popup.getLocation())
                        .address(popup.getLocation())
                        .thumbnailUrl(popup.getThumbnailUrl())
                        .hThumbnailUrl(popup.getThumbnailUrl())
                        .vThumbnailUrl(popup.getThumbnailUrl())
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