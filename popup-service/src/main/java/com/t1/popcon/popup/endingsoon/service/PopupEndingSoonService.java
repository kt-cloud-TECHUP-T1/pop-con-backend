package com.t1.popcon.popup.endingsoon.service;

import com.t1.popcon.popup.detail.entity.Popup;
import com.t1.popcon.popup.dto.card.PhaseStatus;
import com.t1.popcon.popup.dto.card.PopupCardDto;
import com.t1.popcon.popup.dto.section.PopupSectionResponse;
import com.t1.popcon.popup.dto.section.SectionKey;
import com.t1.popcon.popup.endingsoon.repository.PopupEndingSoonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PopupEndingSoonService {

    private static final ZoneOffset KST_OFFSET = ZoneOffset.ofHours(9);

    private final PopupEndingSoonRepository popupEndingSoonRepository;

    public PopupSectionResponse<PopupCardDto> getEndingSoonPopups(int limit) {
        LocalDate now = LocalDate.now();
        LocalDate deadline = now.plusDays(3);

        List<PopupCardDto> items = popupEndingSoonRepository.findEndingSoonPopups(
                        now,
                        deadline,
                        PageRequest.of(0, limit)
                ).stream()
                .map(this::toPopupCardDto)
                .toList();

        return new PopupSectionResponse<>(SectionKey.ENDING_SOON, items.size(), items);
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
                        phaseOpenAt.atOffset(KST_OFFSET),
                        phaseCloseAt.atOffset(KST_OFFSET)
                )
        );
    }


    private PhaseStatus calculatePhaseStatus(LocalDateTime openAt, LocalDateTime closeAt) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(openAt)) return PhaseStatus.UPCOMING;
        if (now.isAfter(closeAt)) return PhaseStatus.CLOSED;
        return PhaseStatus.OPEN;
    }
}
