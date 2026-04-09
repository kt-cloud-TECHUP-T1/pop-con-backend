package com.t1.popcon.popup.listings.service;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.popup.detail.entity.Popup;
import com.t1.popcon.popup.dto.card.OverlayType;
import com.t1.popcon.popup.dto.card.PhaseStatus;
import com.t1.popcon.popup.dto.card.PhaseType;
import com.t1.popcon.popup.dto.card.PopupCardDto;
import com.t1.popcon.popup.dto.card.PopupSort;
import com.t1.popcon.popup.dto.section.PopupSectionResponse;
import com.t1.popcon.popup.dto.section.SectionKey;
import com.t1.popcon.popup.likes.service.PopupLikeReadService;
import com.t1.popcon.popup.listings.repository.PopupListingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PopupListingsService {

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    private final PopupListingsRepository popupListingsRepository;
    private final PopupLikeReadService popupLikeReadService;

    public PopupSectionResponse<PopupCardDto> getPopups(
        Long userId,
        PhaseType phaseType,
        List<PhaseStatus> statuses,
        PopupSort sort,
        int limit
    ) {
        if (phaseType == null || statuses == null || statuses.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        if (limit < 1) {
            Map<String, String> errors = new LinkedHashMap<>();
            errors.put("limit", "limit는 1 이상이어야 합니다.");
            throw new CustomException(ErrorCode.INVALID_INPUT, errors);
        }

        SectionKey sectionKey = resolveSectionKey(phaseType, statuses);
        LocalDateTime now = LocalDateTime.now(KST_ZONE);

        boolean openEnabled = statuses.contains(PhaseStatus.OPEN);
        boolean upcomingEnabled = statuses.contains(PhaseStatus.UPCOMING);
        boolean closedEnabled = statuses.contains(PhaseStatus.CLOSED);

        List<Popup> popups;
        if (phaseType == PhaseType.AUCTION) {
            popups = popupListingsRepository.findAuctionPopups(
                now, openEnabled, upcomingEnabled, closedEnabled,
                PageRequest.of(0, limit)
            );
        } else {
            popups = popupListingsRepository.findDrawPopups(
                now, openEnabled, upcomingEnabled, closedEnabled,
                PageRequest.of(0, limit)
            );
            log.info("[DRAW_LISTINGS] statuses={}, now={}, 조회 결과={}건", statuses, now, popups.size());
        }

        Set<Long> likedPopupIds = popupLikeReadService.getLikedPopupIds(
            userId,
            popups.stream().map(Popup::getId).toList()
        );

        List<PopupCardDto> items = popups.stream()
            .map(p -> toPopupCardDto(p, phaseType, now, likedPopupIds.contains(p.getId())))
            .toList();

        return new PopupSectionResponse<>(sectionKey, items.size(), items);
    }

    public String getMessage(PhaseType type, List<PhaseStatus> statuses) {
        if (type == null || statuses == null || statuses.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        if (type == PhaseType.AUCTION) {
            return "더치 경매 섹션 조회를 성공했습니다.";
        }

        if (statuses.size() == 1 && statuses.contains(PhaseStatus.OPEN)) {
            return "진행 중 드로우 섹션 조회를 성공했습니다.";
        }

        if (statuses.size() == 1 && statuses.contains(PhaseStatus.UPCOMING)) {
            return "오픈 예정 드로우 섹션 조회를 성공했습니다.";
        }

        return "드로우 섹션 조회를 성공했습니다.";
    }

    private SectionKey resolveSectionKey(PhaseType type, List<PhaseStatus> statuses) {
        if (type == PhaseType.AUCTION) {
            return SectionKey.AUCTIONS;
        }

        if (statuses.size() == 1 && statuses.contains(PhaseStatus.OPEN)) {
            return SectionKey.DRAWS_OPEN;
        }

        if (statuses.size() == 1 && statuses.contains(PhaseStatus.UPCOMING)) {
            return SectionKey.DRAWS_UPCOMING;
        }

        throw new CustomException(ErrorCode.INVALID_INPUT);
    }

    private PopupCardDto toPopupCardDto(Popup popup, PhaseType phaseType, LocalDateTime now, boolean liked) {
        LocalDateTime openAt;
        LocalDateTime closeAt;

        if (phaseType == PhaseType.AUCTION) {
            openAt = popup.getAuctionOpenAt();
            closeAt = popup.getAuctionCloseAt();
        } else {
            openAt = popup.getDrawOpenAt();
            closeAt = popup.getDrawCloseAt();
        }

        PhaseStatus status = calculatePhaseStatus(openAt, closeAt, now);

        return new PopupCardDto(
                popup.getId(),
                popup.getTitle(),
                popup.getSubtitle(),
                popup.getSubText() != null ? popup.getSubText() : popup.getLocation(),
                popup.getCaption(),
                phaseType == PhaseType.AUCTION ? popup.getHThumbUrl() : popup.getVThumbUrl(),
                liked,
                new PopupCardDto.StatsDto(popup.getLikeCount(), popup.getViewCount()),
                resolveOverlay(phaseType, status),
                new PopupCardDto.PhaseDto(
                        phaseType,
                        status,
                        openAt.atZone(KST_ZONE).toOffsetDateTime(),
                        closeAt.atZone(KST_ZONE).toOffsetDateTime()
            )
        );
    }

    private PhaseStatus calculatePhaseStatus(LocalDateTime openAt, LocalDateTime closeAt, LocalDateTime now) {
        if (now.isBefore(openAt)) return PhaseStatus.UPCOMING;
        if (!now.isBefore(closeAt)) return PhaseStatus.CLOSED;
        return PhaseStatus.OPEN;
    }

    private PopupCardDto.OverlayDto resolveOverlay(PhaseType phaseType, PhaseStatus status) {
        if (phaseType == PhaseType.AUCTION) {
            if (status == PhaseStatus.OPEN) return new PopupCardDto.OverlayDto(OverlayType.AUCTION_IN_PROGRESS, null);
            if (status == PhaseStatus.UPCOMING) return new PopupCardDto.OverlayDto(OverlayType.AUCTION_OPEN_AT, null);
        }
        if (phaseType == PhaseType.DRAW && status == PhaseStatus.UPCOMING) {
            return new PopupCardDto.OverlayDto(OverlayType.DRAW_OPEN_AT, null);
        }
        return null;
    }
}
