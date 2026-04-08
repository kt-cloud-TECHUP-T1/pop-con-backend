package com.t1.popcon.popup.likes.service;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.popup.detail.entity.Popup;
import com.t1.popcon.popup.detail.entity.PopupLike;
import com.t1.popcon.popup.dto.card.OverlayType;
import com.t1.popcon.popup.dto.card.PhaseStatus;
import com.t1.popcon.popup.dto.card.PhaseType;
import com.t1.popcon.popup.dto.card.PopupCardDto;
import com.t1.popcon.popup.likes.dto.SliceResponse;
import com.t1.popcon.popup.likes.repository.PopupLikeRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PopupLikeQueryService {

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 12;

    private final PopupLikeRepository popupLikeRepository;

    public SliceResponse<PopupCardDto> getLikedPopups(Long userId, Integer page, Integer size) {
        if (userId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        int validatedPage = page == null ? DEFAULT_PAGE : page;
        int validatedSize = size == null ? DEFAULT_SIZE : size;
        validatePageRequest(validatedPage, validatedSize);

        Slice<PopupLike> likeSlice = popupLikeRepository.findByUserIdOrderByCreatedAtDescIdDesc(
            userId,
            PageRequest.of(validatedPage, validatedSize)
        );

        Slice<PopupCardDto> cardSlice = likeSlice.map(popupLike -> toPopupCardDto(popupLike.getPopup()));
        return SliceResponse.from(cardSlice);
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0 || size < 1) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private PopupCardDto toPopupCardDto(Popup popup) {
        PhaseInfo phaseInfo = resolvePhaseInfo(popup);
        PhaseStatus status = calculatePhaseStatus(phaseInfo.openAt(), phaseInfo.closeAt());

        return new PopupCardDto(
            popup.getId(),
            popup.getTitle(),
            popup.getSubtitle(),
            popup.getSubText() != null ? popup.getSubText() : popup.getLocation(),
            popup.getCaption(),
            phaseInfo.phaseType() == PhaseType.AUCTION ? popup.getHThumbUrl() : popup.getVThumbUrl(),
            true,
            new PopupCardDto.StatsDto(popup.getLikeCount(), popup.getViewCount()),
            resolveOverlay(phaseInfo.phaseType(), status),
            new PopupCardDto.PhaseDto(
                phaseInfo.phaseType(),
                status,
                phaseInfo.openAt().atZone(KST_ZONE).toOffsetDateTime(),
                phaseInfo.closeAt().atZone(KST_ZONE).toOffsetDateTime()
            )
        );
    }

    private PhaseInfo resolvePhaseInfo(Popup popup) {
        LocalDateTime now = LocalDateTime.now(KST_ZONE);
        PhaseStatus auctionStatus = calculatePhaseStatus(popup.getAuctionOpenAt(), popup.getAuctionCloseAt(), now);
        PhaseStatus drawStatus = calculatePhaseStatus(popup.getDrawOpenAt(), popup.getDrawCloseAt(), now);

        if (auctionStatus == PhaseStatus.OPEN) {
            return new PhaseInfo(PhaseType.AUCTION, popup.getAuctionOpenAt(), popup.getAuctionCloseAt());
        }

        if (drawStatus == PhaseStatus.OPEN) {
            return new PhaseInfo(PhaseType.DRAW, popup.getDrawOpenAt(), popup.getDrawCloseAt());
        }

        if (auctionStatus == PhaseStatus.UPCOMING && drawStatus != PhaseStatus.UPCOMING) {
            return new PhaseInfo(PhaseType.AUCTION, popup.getAuctionOpenAt(), popup.getAuctionCloseAt());
        }

        if (drawStatus == PhaseStatus.UPCOMING && auctionStatus != PhaseStatus.UPCOMING) {
            return new PhaseInfo(PhaseType.DRAW, popup.getDrawOpenAt(), popup.getDrawCloseAt());
        }

        if (auctionStatus == PhaseStatus.UPCOMING) {
            boolean auctionFirst = popup.getAuctionOpenAt().isBefore(popup.getDrawOpenAt());
            return auctionFirst
                ? new PhaseInfo(PhaseType.AUCTION, popup.getAuctionOpenAt(), popup.getAuctionCloseAt())
                : new PhaseInfo(PhaseType.DRAW, popup.getDrawOpenAt(), popup.getDrawCloseAt());
        }

        boolean auctionLast = popup.getAuctionCloseAt().isAfter(popup.getDrawCloseAt());
        return auctionLast
            ? new PhaseInfo(PhaseType.AUCTION, popup.getAuctionOpenAt(), popup.getAuctionCloseAt())
            : new PhaseInfo(PhaseType.DRAW, popup.getDrawOpenAt(), popup.getDrawCloseAt());
    }

    private PhaseStatus calculatePhaseStatus(LocalDateTime openAt, LocalDateTime closeAt) {
        return calculatePhaseStatus(openAt, closeAt, LocalDateTime.now(KST_ZONE));
    }

    private PhaseStatus calculatePhaseStatus(LocalDateTime openAt, LocalDateTime closeAt, LocalDateTime now) {
        if (openAt == null || closeAt == null) {
            return PhaseStatus.CLOSED;
        }
        if (now.isBefore(openAt)) {
            return PhaseStatus.UPCOMING;
        }
        if (!now.isBefore(closeAt)) {
            return PhaseStatus.CLOSED;
        }
        return PhaseStatus.OPEN;
    }

    private PopupCardDto.OverlayDto resolveOverlay(PhaseType phaseType, PhaseStatus status) {
        if (phaseType == PhaseType.AUCTION) {
            if (status == PhaseStatus.OPEN) {
                return new PopupCardDto.OverlayDto(OverlayType.AUCTION_IN_PROGRESS, null);
            }
            if (status == PhaseStatus.UPCOMING) {
                return new PopupCardDto.OverlayDto(OverlayType.AUCTION_OPEN_AT, null);
            }
        }

        if (phaseType == PhaseType.DRAW && status == PhaseStatus.UPCOMING) {
            return new PopupCardDto.OverlayDto(OverlayType.DRAW_OPEN_AT, null);
        }

        return null;
    }

    private record PhaseInfo(
        PhaseType phaseType,
        LocalDateTime openAt,
        LocalDateTime closeAt
    ) {
    }
}
