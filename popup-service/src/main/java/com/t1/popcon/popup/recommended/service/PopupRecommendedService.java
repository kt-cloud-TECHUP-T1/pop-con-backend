package com.t1.popcon.popup.recommended.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.t1.popcon.popup.recommended.repository.PopupRecommendedRepository;
import com.t1.popcon.popup.dto.card.PopupCardDto;
import com.t1.popcon.popup.dto.card.PopupMapper;
import com.t1.popcon.popup.dto.section.PopupSectionResponse;
import com.t1.popcon.popup.dto.section.SectionKey;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PopupRecommendedService {

	private final PopupRecommendedRepository popupRecommendedRepository;

	public PopupSectionResponse<PopupCardDto> recommended() {
		// 1. 랜덤 10개 조회
		var popups = popupRecommendedRepository.findRandomPopups(LocalDate.now(), 10);

		// 2. 엔티티 -> DTO 변환
		List<PopupCardDto> recommendedItems = popups.stream()
			.map(p -> PopupMapper.toCardDto(p, null))
			.toList();

		// 3. 섹션 응답으로 감싸서 반환
		return new PopupSectionResponse<>(
			SectionKey.RECOMMENDED,
			recommendedItems.size(),
			recommendedItems
		);
	}
}