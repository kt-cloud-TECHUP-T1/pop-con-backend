package com.t1.popcon.popup.dto.card;

import com.t1.popcon.popup.detail.entity.Popup;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class PopupMapper {

    private static final ZoneId TIME_ZONE = ZoneId.of("Asia/Seoul");

    private PopupMapper() {
    }

    public record PhaseInfo(
        PhaseType type,
        PhaseStatus status,
        LocalDateTime openAt,
        LocalDateTime closeAt
    ) {
    }

    public static PhaseInfo resolvePhase(Popup popup, LocalDateTime now) {
        if (now.isBefore(popup.getAuctionCloseAt())) {
            PhaseStatus status = now.isBefore(popup.getAuctionOpenAt()) ? PhaseStatus.UPCOMING : PhaseStatus.OPEN;
            return new PhaseInfo(PhaseType.AUCTION, status, popup.getAuctionOpenAt(), popup.getAuctionCloseAt());
        }

        PhaseStatus status;
        if (now.isBefore(popup.getDrawOpenAt())) {
            status = PhaseStatus.UPCOMING;
        } else if (now.isBefore(popup.getDrawCloseAt())) {
            status = PhaseStatus.OPEN;
        } else {
            status = PhaseStatus.CLOSED;
        }
        return new PhaseInfo(PhaseType.DRAW, status, popup.getDrawOpenAt(), popup.getDrawCloseAt());
    }

    public static PopupCardDto toCardDto(Popup popup, Integer rank, boolean liked) {
        PhaseInfo phaseInfo = resolvePhase(popup, LocalDateTime.now(TIME_ZONE));
        return toCardDto(
            popup,
            popup.getSubtitle(),
            popup.getSubText() != null ? popup.getSubText() : popup.getLocation(),
            popup.getVThumbUrl(),
            liked,
            rank != null ? new PopupCardDto.OverlayDto(OverlayType.RANK, rank) : null,
            phaseInfo.type(),
            phaseInfo.status(),
            phaseInfo.openAt(),
            phaseInfo.closeAt()
        );
    }

    public static PopupCardDto toCardDto(
        Popup popup,
        boolean liked,
        PhaseType phaseType,
        PhaseStatus phaseStatus,
        LocalDateTime openAt,
        LocalDateTime closeAt
    ) {
        return toCardDto(
            popup,
            popup.getSubtitle(),
            popup.getSubText() != null ? popup.getSubText() : popup.getLocation(),
            popup.getVThumbUrl(),
            liked,
            null,
            phaseType,
            phaseStatus,
            openAt,
            closeAt
        );
    }

    private static PopupCardDto toCardDto(
        Popup popup,
        String supportingText,
        String subText,
        String thumbnailUrl,
        boolean liked,
        PopupCardDto.OverlayDto overlay,
        PhaseType phaseType,
        PhaseStatus phaseStatus,
        LocalDateTime openAt,
        LocalDateTime closeAt
    ) {
        return new PopupCardDto(
            popup.getId(),
            popup.getTitle(),
            supportingText,
            subText,
            popup.getCaption(),
            thumbnailUrl,
            liked,
            new PopupCardDto.StatsDto(popup.getLikeCount(), popup.getViewCount()),
            overlay,
            new PopupCardDto.PhaseDto(
                phaseType,
                phaseStatus,
                openAt.atZone(TIME_ZONE).toOffsetDateTime(),
                closeAt.atZone(TIME_ZONE).toOffsetDateTime()
            )
        );
    }
}
