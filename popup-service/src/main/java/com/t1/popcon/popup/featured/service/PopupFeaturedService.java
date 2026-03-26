package com.t1.popcon.popup.featured.service;

import com.t1.popcon.popup.detail.entity.Popup;
import com.t1.popcon.popup.dto.card.PhaseStatus;
import com.t1.popcon.popup.dto.card.PopupCardDto;
import com.t1.popcon.popup.dto.section.PopupSectionResponse;
import com.t1.popcon.popup.dto.section.SectionKey;
import com.t1.popcon.popup.featured.repository.PopupFeaturedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PopupFeaturedService {

    private static final long LIKE_WEIGHT = 3L;
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    private final PopupFeaturedRepository popupFeaturedRepository;

    public PopupSectionResponse<PopupCardDto> getFeaturedPopups(int limit) {
        List<PopupCardDto> items = popupFeaturedRepository.findFeaturedPopups(
                        LIKE_WEIGHT,
                        PageRequest.of(0, limit)
                ).stream()
                .map(this::toPopupCardDto)
                .toList();

        return new PopupSectionResponse<>(SectionKey.FEATURED, items.size(), items);
    }

    private PopupCardDto toPopupCardDto(Popup popup) {
        LocalDateTime phaseOpenAt = popup.getOpenAt().atStartOfDay();
        LocalDateTime phaseCloseAt = popup.getCloseAt().atTime(LocalTime.MAX);

        return new PopupCardDto(
                popup.getId(),
                popup.getTitle(),
                popup.getSubtitle(),
                popup.getSubText() != null ? popup.getSubText() : popup.getLocation(),
                popup.getCaption(),
                popup.getThumbnailUrl(),
                false,
                new PopupCardDto.StatsDto(
                        popup.getLikeCount(),
                        popup.getViewCount()
                ),
                null,
                new PopupCardDto.PhaseDto(
                        popup.getPhaseType(),
                        calculatePhaseStatus(phaseOpenAt, phaseCloseAt),
                        phaseOpenAt.atZone(KST_ZONE).toOffsetDateTime(),
                        phaseCloseAt.atZone(KST_ZONE).toOffsetDateTime()
                )
        );
    }

    private PhaseStatus calculatePhaseStatus(LocalDateTime openAt, LocalDateTime closeAt) {
        LocalDateTime now = LocalDateTime.now(KST_ZONE);
        if (now.isBefore(openAt)) return PhaseStatus.UPCOMING;
        if (now.isAfter(closeAt)) return PhaseStatus.CLOSED;
        return PhaseStatus.OPEN;
    }
}