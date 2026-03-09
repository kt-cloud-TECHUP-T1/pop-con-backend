package com.t1.popcon.popup.endingsoon.service;

import com.t1.popcon.popup.detail.entity.Popup;
import com.t1.popcon.popup.dto.card.PhaseStatus;
import com.t1.popcon.popup.dto.card.PhaseType;
import com.t1.popcon.popup.dto.card.PopupCardDto;
import com.t1.popcon.popup.dto.section.PopupSectionResponse;
import com.t1.popcon.popup.dto.section.SectionKey;
import com.t1.popcon.popup.endingsoon.repository.PopupEndingSoonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PopupEndingSoonService {

    private static final ZoneOffset KST_OFFSET = ZoneOffset.ofHours(9);

    private final PopupEndingSoonRepository popupEndingSoonRepository;

    public PopupSectionResponse<PopupCardDto> getEndingSoonPopups(int limit) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusDays(3);

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
        PhaseType phaseType = popup.getPhaseType();
        LocalDateTime phaseOpenAt = getPhaseOpenAt(popup, phaseType);
        LocalDateTime phaseCloseAt = getPhaseCloseAt(popup, phaseType);

        return new PopupCardDto(
                popup.getId(),
                popup.getTitle(),
                null,
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
                        phaseType,
                        calculatePhaseStatus(phaseOpenAt, phaseCloseAt),
                        phaseOpenAt.atOffset(KST_OFFSET),
                        phaseCloseAt.atOffset(KST_OFFSET)
                )
        );
    }

    private LocalDateTime getPhaseOpenAt(Popup popup, PhaseType phaseType) {
        return phaseType == PhaseType.AUCTION ? popup.getAuctionOpenAt() : popup.getDrawOpenAt();
    }

    private LocalDateTime getPhaseCloseAt(Popup popup, PhaseType phaseType) {
        return phaseType == PhaseType.AUCTION ? popup.getAuctionCloseAt() : popup.getDrawCloseAt();
    }

    private PhaseStatus calculatePhaseStatus(LocalDateTime openAt, LocalDateTime closeAt) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(openAt)) return PhaseStatus.UPCOMING;
        if (now.isAfter(closeAt)) return PhaseStatus.CLOSED;
        return PhaseStatus.OPEN;
    }
}