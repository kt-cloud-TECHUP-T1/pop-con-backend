package com.t1.popcon.popup.featured.service;

import com.t1.popcon.popup.detail.entity.Popup;
import com.t1.popcon.popup.dto.card.PhaseStatus;
import com.t1.popcon.popup.dto.card.PhaseType;
import com.t1.popcon.popup.dto.card.PopupCardDto;
import com.t1.popcon.popup.dto.section.PopupSectionResponse;
import com.t1.popcon.popup.dto.section.SectionKey;
import com.t1.popcon.popup.featured.repository.PopupFeaturedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PopupFeaturedService {

    private static final long LIKE_WEIGHT = 3L;
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    private final PopupFeaturedRepository popupFeaturedRepository;

    public PopupSectionResponse<PopupCardDto> getFeaturedPopups(int limit) {
        List<PopupCardDto> items = popupFeaturedRepository.findFeaturedPopups(
                        LIKE_WEIGHT,
                        PageRequest.of(0, limit)
                ).stream()
                .map(this::toPopupCardDto)
                .toList();

        return new PopupSectionResponse<>(SectionKey.FEATURED, items.size(), items);
    }

    private PopupCardDto toPopupCardDto(Popup popup) {
        PhaseInfo phaseInfo = resolvePhaseInfo(popup);

        return new PopupCardDto(
                popup.getId(),
                popup.getTitle(),
                popup.getSubtitle(),
                popup.getSubText() != null ? popup.getSubText() : popup.getLocation(),
                popup.getCaption(),
                popup.getVThumbUrl(),
                false,
                new PopupCardDto.StatsDto(
                        popup.getLikeCount(),
                        popup.getViewCount()
                ),
                null,
                new PopupCardDto.PhaseDto(
                        phaseInfo.phaseType(),
                        calculatePhaseStatus(phaseInfo.openAt(), phaseInfo.closeAt()),
                        phaseInfo.openAt().atZone(KST_ZONE).toOffsetDateTime(),
                        phaseInfo.closeAt().atZone(KST_ZONE).toOffsetDateTime()
                )
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
            return new PhaseInfo(
                    PhaseType.AUCTION,
                    popup.getAuctionOpenAt(),
                    popup.getAuctionCloseAt()
            );
        }

        if (drawActive) {
            return new PhaseInfo(
                    PhaseType.DRAW,
                    popup.getDrawOpenAt(),
                    popup.getDrawCloseAt()
            );
        }

        if (auctionExists && drawExists) {
            return popup.getAuctionOpenAt().isBefore(popup.getDrawOpenAt())
                    ? new PhaseInfo(PhaseType.AUCTION, popup.getAuctionOpenAt(), popup.getAuctionCloseAt())
                    : new PhaseInfo(PhaseType.DRAW, popup.getDrawOpenAt(), popup.getDrawCloseAt());
        }

        if (auctionExists) {
            return new PhaseInfo(
                    PhaseType.AUCTION,
                    popup.getAuctionOpenAt(),
                    popup.getAuctionCloseAt()
            );
        }

        if (drawExists) {
            return new PhaseInfo(
                    PhaseType.DRAW,
                    popup.getDrawOpenAt(),
                    popup.getDrawCloseAt()
            );
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