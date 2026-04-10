package com.t1.popcon.popup.recommended.service;

import com.t1.popcon.popup.detail.entity.Popup;
import com.t1.popcon.popup.dto.card.PopupCardDto;
import com.t1.popcon.popup.dto.card.PopupMapper;
import com.t1.popcon.popup.dto.section.PopupSectionResponse;
import com.t1.popcon.popup.dto.section.SectionKey;
import com.t1.popcon.popup.likes.service.PopupLikeReadService;
import com.t1.popcon.popup.recommended.repository.PopupRecommendedRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PopupRecommendedService {

	private final PopupRecommendedRepository popupRecommendedRepository;
	private final PopupLikeReadService popupLikeReadService;
	private static final ZoneId TIME_ZONE = ZoneId.of("Asia/Seoul");

	public PopupSectionResponse<PopupCardDto> recommended(Long userId) {
		List<Popup> allPopups = popupRecommendedRepository.findAllByCloseAtAfter(LocalDate.now(TIME_ZONE));
		List<Popup> shuffledPopups = new ArrayList<>(allPopups);
		Collections.shuffle(shuffledPopups);

		List<Popup> randomPopups = shuffledPopups.stream()
			.limit(10)
			.toList();

		Set<Long> likedPopupIds = popupLikeReadService.getLikedPopupIds(
			userId,
			randomPopups.stream().map(Popup::getId).toList()
		);

		List<PopupCardDto> recommendedItems = randomPopups.stream()
			.map(p -> PopupMapper.toCardDto(p, null, likedPopupIds.contains(p.getId())))
			.toList();

		return new PopupSectionResponse<>(
			SectionKey.RECOMMENDED,
			recommendedItems.size(),
			recommendedItems
		);
	}
}
