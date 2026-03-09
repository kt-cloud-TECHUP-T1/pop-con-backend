package com.t1.popcon.popup.rankings.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;

import com.t1.popcon.popup.dto.card.OverlayType;
import com.t1.popcon.popup.dto.card.PhaseStatus;
import com.t1.popcon.popup.dto.card.PhaseType;
import com.t1.popcon.popup.dto.card.PopupCardDto;
import com.t1.popcon.popup.dto.section.PopupSectionResponse;
import com.t1.popcon.popup.dto.section.SectionKey;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PopupRankingsService {

	public PopupSectionResponse<PopupCardDto> getWeeklyRankings() {
		// 1. 1위부터 5위까지의 목업 데이터 생성
		List<PopupCardDto> mockItems = IntStream.rangeClosed(1, 5)
			.mapToObj(this::createMockRankingCard)
			.toList();

		return new PopupSectionResponse<>(
			SectionKey.RANKINGS_WEEKLY,
			mockItems.size(),
			mockItems
		);
	}

	private PopupCardDto createMockRankingCard(int rank) {
		return new PopupCardDto(
			(long) rank,
			"Title",
			null, // supportingText
			"SubText",
			"Caption",
			"https://.../thumb.jpg",
			true, // liked
			new PopupCardDto.StatsDto(0, 0),
			new PopupCardDto.OverlayDto(OverlayType.RANK, rank), // 랭킹 오버레이 설정
			new PopupCardDto.PhaseDto(
				PhaseType.AUCTION,
				PhaseStatus.UPCOMING,
				OffsetDateTime.parse("2026-02-23T10:30:00+09:00"),
				OffsetDateTime.parse("2026-02-24T10:30:00+09:00")
			)
		);
	}
}