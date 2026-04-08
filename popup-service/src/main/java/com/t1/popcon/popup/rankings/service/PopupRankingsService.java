package com.t1.popcon.popup.rankings.service;

import com.t1.popcon.popup.dto.card.PopupCardDto;
import com.t1.popcon.popup.dto.card.PopupMapper;
import com.t1.popcon.popup.dto.section.PopupSectionResponse;
import com.t1.popcon.popup.dto.section.SectionKey;
import com.t1.popcon.popup.likes.service.PopupLikeReadService;
import com.t1.popcon.popup.rankings.repository.PopupRankingsRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PopupRankingsService {

	private final PopupRankingsRepository popupRankingsRepository;
	private final PopupLikeReadService popupLikeReadService;
	private static final ZoneId TIME_ZONE = ZoneId.of("Asia/Seoul");

	@Cacheable(
		value = "popularRankings",
		key = "T(java.time.LocalDate).now(T(java.time.ZoneId).of('Asia/Seoul')).toString() + ':' + (#userId == null ? 'guest' : #userId)"
	)
	public PopupSectionResponse<PopupCardDto> getPopularRankings(Long userId) {
		var popups = popupRankingsRepository.findPopupsByWeightedScore(LocalDate.now(TIME_ZONE), PageRequest.of(0, 10));
		Set<Long> likedPopupIds = popupLikeReadService.getLikedPopupIds(
			userId,
			popups.stream().map(p -> p.getId()).toList()
		);

		List<PopupCardDto> rankingItems = IntStream.range(0, popups.size())
			.mapToObj(i -> PopupMapper.toCardDto(popups.get(i), i + 1, likedPopupIds.contains(popups.get(i).getId())))
			.toList();

		return new PopupSectionResponse<>(
			SectionKey.RANKINGS_WEEKLY,
			rankingItems.size(),
			rankingItems
		);
	}
}
