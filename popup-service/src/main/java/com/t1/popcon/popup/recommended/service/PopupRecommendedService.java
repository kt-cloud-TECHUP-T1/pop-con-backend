package com.t1.popcon.popup.recommended.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.t1.popcon.popup.recommended.repository.PopupRecommendedRepository;
import com.t1.popcon.popup.detail.entity.Popup;
import com.t1.popcon.popup.dto.card.PopupCardDto;
import com.t1.popcon.popup.dto.card.PopupMapper;
import com.t1.popcon.popup.dto.section.PopupSectionResponse;
import com.t1.popcon.popup.dto.section.SectionKey;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PopupRecommendedService {

	private final PopupRecommendedRepository popupRecommendedRepository;
	private static final ZoneId TIME_ZONE = ZoneId.of("Asia/Seoul");

	public PopupSectionResponse<PopupCardDto> recommended() {
		// 1. 조건에 맞는 전체 팝업 조회
		List<Popup> allPopups = popupRecommendedRepository.findAllByCloseAtAfter(LocalDate.now(TIME_ZONE));

		// 2. 자바에서 리스트 복사 후 셔플 (원본 보존)
		List<Popup> shuffledPopups = new ArrayList<>(allPopups);
		Collections.shuffle(shuffledPopups);

		// 3. 내부적으로 10개만 추출하도록 고정
		List<Popup> randomPopups = shuffledPopups.stream()
			.limit(10)
			.toList();

		// 4. 엔티티 -> DTO 변환
		List<PopupCardDto> recommendedItems = randomPopups.stream()
			.map(p -> PopupMapper.toCardDto(p, null))
			.toList();

		return new PopupSectionResponse<>(
			SectionKey.RECOMMENDED,
			recommendedItems.size(),
			recommendedItems
		);
	}
}
