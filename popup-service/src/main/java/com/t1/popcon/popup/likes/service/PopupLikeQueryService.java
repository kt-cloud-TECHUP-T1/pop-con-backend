package com.t1.popcon.popup.likes.service;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.popup.detail.entity.Popup;
import com.t1.popcon.popup.detail.entity.PopupLike;
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
    private static final int MAX_PAGE_SIZE = 100;

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
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private PopupCardDto toPopupCardDto(Popup popup) {
        PhaseInfo phaseInfo = resolvePhaseInfo(popup);
        PhaseStatus status = phaseInfo == null
          ? PhaseStatus.CLOSED
          : calculatePhaseStatus(phaseInfo.openAt(), phaseInfo.closeAt());
        PopupCardDto.PhaseDto phaseDto = phaseInfo == null
          ? null
          : new PopupCardDto.PhaseDto(
          phaseInfo.phaseType(),
          status,
          toOffsetDateTime(phaseInfo.openAt()),
          toOffsetDateTime(phaseInfo.closeAt())
        );

        return new PopupCardDto(
          popup.getId(),
          popup.getTitle(),
          popup.getSubtitle(),
          popup.getSubText() != null ? popup.getSubText() : popup.getLocation(),
          popup.getCaption(),
          phaseInfo != null && phaseInfo.phaseType() == PhaseType.AUCTION ? popup.getHThumbUrl() : popup.getVThumbUrl(),
          true,
          new PopupCardDto.StatsDto(popup.getLikeCount(), popup.getViewCount()),
          null,
          phaseDto
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

        if (popup.getAuctionCloseAt() == null && popup.getDrawCloseAt() == null) {
            return null;
        }

        if (popup.getAuctionCloseAt() == null) {
            return new PhaseInfo(PhaseType.DRAW, popup.getDrawOpenAt(), popup.getDrawCloseAt());
        }

        if (popup.getDrawCloseAt() == null) {
            return new PhaseInfo(PhaseType.AUCTION, popup.getAuctionOpenAt(), popup.getAuctionCloseAt());
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

    private java.time.OffsetDateTime toOffsetDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atZone(KST_ZONE).toOffsetDateTime();
    }

    private record PhaseInfo(
        PhaseType phaseType,
        LocalDateTime openAt,
        LocalDateTime closeAt
    ) {
    }

    public long countLikedPopupsByUserId(Long userId) {
        if (userId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        return popupLikeRepository.countByUserId(userId);
    }
}
