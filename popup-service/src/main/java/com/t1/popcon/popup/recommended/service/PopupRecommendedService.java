package com.t1.popcon.popup.recommended.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;

import com.t1.popcon.popup.dto.card.PhaseStatus;
import com.t1.popcon.popup.dto.card.PhaseType;
import com.t1.popcon.popup.dto.card.PopupCardDto;
import com.t1.popcon.popup.dto.section.PopupSectionResponse;
import com.t1.popcon.popup.dto.section.SectionKey;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PopupRecommendedService {

	public PopupSectionResponse<PopupCardDto> recommended() {
		// 1. 4개의 목업 데이터 생성
		List<PopupCardDto> mockItems = IntStream.rangeClosed(1, 4)
			.mapToObj(this::createMockCard)
			.toList();

		// 2. 섹션 응답으로 감싸서 반환
		return new PopupSectionResponse<>(
			SectionKey.RECOMMENDED,
			mockItems.size(),
			mockItems
		);
	}

	private PopupCardDto createMockCard(int id) {
		return new PopupCardDto(
			(long) id,
			"Title " + id,
			null, // supportingText
			"SubText",
			"Caption",
			"https://.../thumb.jpg",
			true, // liked
			new PopupCardDto.StatsDto(0, 0),
			null, // overlay
			new PopupCardDto.PhaseDto(
				PhaseType.AUCTION,
				PhaseStatus.UPCOMING,
				OffsetDateTime.parse("2026-02-23T10:30:00+09:00"),
				OffsetDateTime.parse("2026-02-24T10:30:00+09:00")
			)
		);
	}
}