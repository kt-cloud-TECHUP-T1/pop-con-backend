package com.t1.popcon.popup.rankings.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.t1.popcon.popup.rankings.repository.PopupRankingsRepository;
import com.t1.popcon.popup.dto.card.PopupCardDto;
import com.t1.popcon.popup.dto.card.PopupMapper;
import com.t1.popcon.popup.dto.section.PopupSectionResponse;
import com.t1.popcon.popup.dto.section.SectionKey;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PopupRankingsService {

	private final PopupRankingsRepository popupRankingsRepository;

	@Cacheable(value = "weeklyRankings")
	public PopupSectionResponse<PopupCardDto> getWeeklyRankings() {
		// 1. 가중치 기반 상위 10개 조회 (다단계 정렬 적용됨)
		var popups = popupRankingsRepository.findTop10ByOrderByWeightedScore(LocalDate.now(), PageRequest.of(0, 10));

		// 2. 엔티티 -> DTO 변환 및 랭킹 오버레이 추가
		List<PopupCardDto> rankingItems = IntStream.range(0, popups.size())
			.mapToObj(i -> PopupMapper.toCardDto(popups.get(i), i + 1))
			.toList();

		return new PopupSectionResponse<>(
			SectionKey.RANKINGS_WEEKLY,
			rankingItems.size(),
			rankingItems
		);
	}
}