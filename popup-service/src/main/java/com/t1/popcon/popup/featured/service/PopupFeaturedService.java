package com.t1.popcon.popup.featured.service;

import com.t1.popcon.popup.detail.entity.Popup;
import com.t1.popcon.popup.dto.card.PhaseStatus;
import com.t1.popcon.popup.dto.card.PhaseType;
import com.t1.popcon.popup.dto.card.PopupCardDto;
import com.t1.popcon.popup.dto.card.PopupMapper;
import com.t1.popcon.popup.dto.section.PopupSectionResponse;
import com.t1.popcon.popup.dto.section.SectionKey;
import com.t1.popcon.popup.featured.repository.PopupFeaturedRepository;
import com.t1.popcon.popup.likes.service.PopupLikeReadService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PopupFeaturedService {

    private static final long LIKE_WEIGHT = 3L;
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    private final PopupFeaturedRepository popupFeaturedRepository;
    private final PopupLikeReadService popupLikeReadService;

    public PopupSectionResponse<PopupCardDto> getFeaturedPopups(Long userId, int limit) {
        List<Popup> popups = popupFeaturedRepository.findFeaturedPopups(
                LIKE_WEIGHT,
                PageRequest.of(0, limit)
        );
        Set<Long> likedPopupIds = popupLikeReadService.getLikedPopupIds(userId, popups.stream().map(Popup::getId).toList());

        List<PopupCardDto> items = popups.stream()
                .map(popup -> toPopupCardDto(popup, likedPopupIds.contains(popup.getId())))
                .toList();

        return new PopupSectionResponse<>(SectionKey.FEATURED, items.size(), items);
    }

    private PopupCardDto toPopupCardDto(Popup popup, boolean liked) {
        PhaseInfo phaseInfo = resolvePhaseInfo(popup);
        return PopupMapper.toCardDto(
                popup,
                liked,
                phaseInfo.phaseType(),
                calculatePhaseStatus(phaseInfo.openAt(), phaseInfo.closeAt()),
                phaseInfo.openAt(),
                phaseInfo.closeAt()
        );
    }

    private PhaseInfo resolvePhaseInfo(Popup popup) {
        LocalDateTime now = LocalDateTime.now(KST_ZONE);

        boolean auctionExists = popup.getAuctionId() != null
                && popup.getAuctionOpenAt() != null
                && popup.getAuctionCloseAt() != null;

        boolean drawExists = popup.getDrawId() != null
                && popup.getDrawOpenAt() != null
                && popup.getDrawCloseAt() != null;

        boolean auctionActive = auctionExists
                && !now.isBefore(popup.getAuctionOpenAt())
                && now.isBefore(popup.getAuctionCloseAt());

        boolean drawActive = drawExists
                && !now.isBefore(popup.getDrawOpenAt())
                && now.isBefore(popup.getDrawCloseAt());

        if (auctionActive) {
            return new PhaseInfo(PhaseType.AUCTION, popup.getAuctionOpenAt(), popup.getAuctionCloseAt());
        }

        if (drawActive) {
            return new PhaseInfo(PhaseType.DRAW, popup.getDrawOpenAt(), popup.getDrawCloseAt());
        }

        if (auctionExists && drawExists) {
            return popup.getAuctionOpenAt().isBefore(popup.getDrawOpenAt())
                    ? new PhaseInfo(PhaseType.AUCTION, popup.getAuctionOpenAt(), popup.getAuctionCloseAt())
                    : new PhaseInfo(PhaseType.DRAW, popup.getDrawOpenAt(), popup.getDrawCloseAt());
        }

        if (auctionExists) {
            return new PhaseInfo(PhaseType.AUCTION, popup.getAuctionOpenAt(), popup.getAuctionCloseAt());
        }

        if (drawExists) {
            return new PhaseInfo(PhaseType.DRAW, popup.getDrawOpenAt(), popup.getDrawCloseAt());
        }

        throw new IllegalStateException("Popup phase information is missing. popupId=" + popup.getId());
    }

    private PhaseStatus calculatePhaseStatus(LocalDateTime openAt, LocalDateTime closeAt) {
        LocalDateTime now = LocalDateTime.now(KST_ZONE);
        if (now.isBefore(openAt)) return PhaseStatus.UPCOMING;
        if (!now.isBefore(closeAt)) return PhaseStatus.CLOSED;
        return PhaseStatus.OPEN;
    }

    private record PhaseInfo(
            PhaseType phaseType,
            LocalDateTime openAt,
            LocalDateTime closeAt
    ) {
    }
}
