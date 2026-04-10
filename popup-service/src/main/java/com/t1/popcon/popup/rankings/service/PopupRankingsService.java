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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PopupRankingsService {

	private final PopupRankingsRepository popupRankingsRepository;
	private final PopupLikeReadService popupLikeReadService;
	private final CacheManager cacheManager;
	private static final ZoneId TIME_ZONE = ZoneId.of("Asia/Seoul");

	public PopupSectionResponse<PopupCardDto> getPopularRankings(Long userId) {
		PopupSectionResponse<PopupCardDto> cachedResponse = getCachedPopularRankings();
		Set<Long> likedPopupIds = userId == null || cachedResponse.items().isEmpty()
			? Set.of()
			: popupLikeReadService.getLikedPopupIds(
				userId,
				cachedResponse.items().stream().map(PopupCardDto::popupId).toList()
			);

		if (likedPopupIds.isEmpty()) {
			return cachedResponse;
		}

		List<PopupCardDto> rankingItems = cachedResponse.items().stream()
			.map(item -> new PopupCardDto(
				item.popupId(),
				item.title(),
				item.supportingText(),
				item.subText(),
				item.caption(),
				item.thumbnailUrl(),
				likedPopupIds.contains(item.popupId()),
				item.stats(),
				item.overlay(),
				item.phase()
			))
			.toList();

		return new PopupSectionResponse<>(
			cachedResponse.sectionKey(),
			cachedResponse.itemCount(),
			rankingItems
		);
	}

	private PopupSectionResponse<PopupCardDto> getCachedPopularRankings() {
		LocalDate currentDate = LocalDate.now(TIME_ZONE);
		String cacheKey = currentDate.toString();
		Cache cache = cacheManager.getCache("popularRankings");
		if (cache != null) {
			PopupSectionResponse<PopupCardDto> cached = cache.get(cacheKey, PopupSectionResponse.class);
			if (cached != null) {
				return cached;
			}
		}

		var popups = popupRankingsRepository.findPopupsByWeightedScore(currentDate, PageRequest.of(0, 10));
		Set<Long> likedPopupIds = popups.isEmpty()
			? Set.of()
			: popupLikeReadService.getLikedPopupIds(
				null,
				popups.stream().map(p -> p.getId()).toList()
			);

		List<PopupCardDto> rankingItems = IntStream.range(0, popups.size())
			.mapToObj(i -> PopupMapper.toCardDto(popups.get(i), i + 1, likedPopupIds.contains(popups.get(i).getId())))
			.toList();

		PopupSectionResponse<PopupCardDto> response = new PopupSectionResponse<>(
			SectionKey.RANKINGS_WEEKLY,
			rankingItems.size(),
			rankingItems
		);
		if (cache != null) {
			cache.put(cacheKey, response);
		}
		return response;
	}
}
