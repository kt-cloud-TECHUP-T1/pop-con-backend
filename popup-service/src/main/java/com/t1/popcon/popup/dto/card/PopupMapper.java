package com.t1.popcon.popup.dto.card;

import java.time.LocalDateTime;
import java.time.ZoneId;

import com.t1.popcon.popup.detail.entity.Popup;

public class PopupMapper {

	public static PopupCardDto toCardDto(Popup popup, Integer rank) {
		LocalDateTime now = LocalDateTime.now();
		PhaseType type;
		PhaseStatus status;
		LocalDateTime openAt;
		LocalDateTime closeAt;

		// 1. 현재 시간에 따른 PhaseType 및 PhaseStatus 결정 로직
		if (now.isBefore(popup.getAuctionCloseAt())) {
			// 경매 종료 전 -> AUCTION 단계
			type = PhaseType.AUCTION;
			if (now.isBefore(popup.getAuctionOpenAt())) {
				status = PhaseStatus.UPCOMING;
			} else {
				status = PhaseStatus.OPEN;
			}
			openAt = popup.getAuctionOpenAt();
			closeAt = popup.getAuctionCloseAt();
		} else {
			// 경매 종료 후 -> DRAW 단계
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
			false,
			new PopupCardDto.StatsDto(popup.getLikeCount(), popup.getViewCount()),
			rank != null ? new PopupCardDto.OverlayDto(OverlayType.RANK, rank) : null,
			new PopupCardDto.PhaseDto(
				type,
				status,
				openAt.atZone(ZoneId.systemDefault()).toOffsetDateTime(),
				closeAt.atZone(ZoneId.systemDefault()).toOffsetDateTime()
			)
		);

	}
}
