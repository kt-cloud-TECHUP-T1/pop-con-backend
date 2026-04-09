package com.t1.popcon.popup.featured.service;

import com.t1.popcon.popup.detail.entity.Popup;
import com.t1.popcon.popup.dto.card.PopupCardDto;
import com.t1.popcon.popup.dto.card.PopupMapper;
import com.t1.popcon.popup.dto.section.PopupSectionResponse;
import com.t1.popcon.popup.dto.section.SectionKey;
import com.t1.popcon.popup.featured.repository.PopupFeaturedRepository;
import com.t1.popcon.popup.likes.service.PopupLikeReadService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PopupFeaturedService {

    private static final long LIKE_WEIGHT = 3L;
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    private final PopupFeaturedRepository popupFeaturedRepository;
    private final PopupLikeReadService popupLikeReadService;

    public PopupSectionResponse<PopupCardDto> getFeaturedPopups(Long userId, int limit) {
        List<Popup> popups = popupFeaturedRepository.findFeaturedPopups(
            LIKE_WEIGHT,
            PageRequest.of(0, limit)
        );
        Set<Long> likedPopupIds = popupLikeReadService.getLikedPopupIds(
            userId,
            popups.stream().map(Popup::getId).toList()
        );

        List<PopupCardDto> items = popups.stream()
            .map(popup -> toPopupCardDto(popup, likedPopupIds.contains(popup.getId())))
            .toList();

        return new PopupSectionResponse<>(SectionKey.FEATURED, items.size(), items);
    }

    private PopupCardDto toPopupCardDto(Popup popup) {
        PopupMapper.PhaseInfo phaseInfo = PopupMapper.resolvePhase(popup, LocalDateTime.now(KST_ZONE));

        return new PopupCardDto(
                popup.getId(),
                popup.getTitle(),
                popup.getSubtitle(),
                popup.getSubText() != null ? popup.getSubText() : popup.getLocation(),
                popup.getCaption(),
                popup.getVThumbUrl(),
                false,
                new PopupCardDto.StatsDto(
                        popup.getLikeCount(),
                        popup.getViewCount()
                ),
                null,
                new PopupCardDto.PhaseDto(
                        phaseInfo.type(),
                        phaseInfo.status(),
                        phaseInfo.openAt().atZone(KST_ZONE).toOffsetDateTime(),
                        phaseInfo.closeAt().atZone(KST_ZONE).toOffsetDateTime()
                )
    private PopupCardDto toPopupCardDto(Popup popup, boolean liked) {
        PopupMapper.PhaseInfo phaseInfo = PopupMapper.resolvePhase(popup, LocalDateTime.now(KST_ZONE));
        return PopupMapper.toCardDto(
            popup,
            liked,
            phaseInfo.type(),
            phaseInfo.status(),
            phaseInfo.openAt(),
            phaseInfo.closeAt()
        );
    }
}
