package com.t1.popcon.popup.dto.card;

import com.t1.popcon.popup.detail.entity.Popup;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class PopupMapper {

	private static final ZoneId TIME_ZONE = ZoneId.of("Asia/Seoul");

	private PopupMapper() {
	}

	public static PopupCardDto toCardDto(Popup popup, Integer rank, boolean liked) {
		LocalDateTime now = LocalDateTime.now(TIME_ZONE);
		PhaseType type;
		PhaseStatus status;
		LocalDateTime openAt;
		LocalDateTime closeAt;

		if (now.isBefore(popup.getAuctionCloseAt())) {
			type = PhaseType.AUCTION;
			if (now.isBefore(popup.getAuctionOpenAt())) {
				status = PhaseStatus.UPCOMING;
			} else {
				status = PhaseStatus.OPEN;
			}
			openAt = popup.getAuctionOpenAt();
			closeAt = popup.getAuctionCloseAt();
		} else {
			type = PhaseType.DRAW;
			if (now.isBefore(popup.getDrawOpenAt())) {
				status = PhaseStatus.UPCOMING;
			} else if (now.isBefore(popup.getDrawCloseAt())) {
				status = PhaseStatus.OPEN;
			} else {
				status = PhaseStatus.CLOSED;
			}
			openAt = popup.getDrawOpenAt();
			closeAt = popup.getDrawCloseAt();
		}

		return new PopupCardDto(
			popup.getId(),
			popup.getTitle(),
			null,
			popup.getSubText(),
			popup.getCaption(),
			popup.getVThumbUrl(),
			liked,
			new PopupCardDto.StatsDto(popup.getLikeCount(), popup.getViewCount()),
			rank != null ? new PopupCardDto.OverlayDto(OverlayType.RANK, rank) : null,
			new PopupCardDto.PhaseDto(
				type,
				status,
				openAt.atZone(TIME_ZONE).toOffsetDateTime(),
				closeAt.atZone(TIME_ZONE).toOffsetDateTime()
			)
		);
	}
}
