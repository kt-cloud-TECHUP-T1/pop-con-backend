package com.t1.popcon.popup.banners.service;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.popup.banners.entity.Banner;
import com.t1.popcon.popup.banners.repository.BannerRepository;
import com.t1.popcon.popup.detail.entity.Popup;
import com.t1.popcon.popup.dto.card.PhaseStatus;
import com.t1.popcon.popup.dto.card.PhaseType;
import com.t1.popcon.popup.dto.card.PopupCardDto;
import com.t1.popcon.popup.dto.section.PopupSectionResponse;
import com.t1.popcon.popup.dto.section.SectionKey;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PopupBannersService {

    private static final ZoneOffset KST_OFFSET = ZoneOffset.ofHours(9);

    private final BannerRepository bannerRepository;

    @Transactional(readOnly = true)
    public PopupSectionResponse<PopupCardDto> getBanners(int limit) {

        if (limit < 1 || limit > 5) {
            Map<String, String> errors = new LinkedHashMap<>();
            errors.put("limit", "limit는 1 이상 5 이하여야 합니다.");
            throw new CustomException(ErrorCode.INVALID_INPUT, errors);
        }

        List<PopupCardDto> items = bannerRepository.findActiveBannersWithPopup(PageRequest.of(0, limit))
                .stream()
                .map(this::toPopupCardDto)
                .toList();

        return new PopupSectionResponse<>(SectionKey.BANNERS, items.size(), items);
    }

    private PopupCardDto toPopupCardDto(Banner banner) {
        Popup popup = banner.getPopup();
        PhaseType phaseType = popup.getPhaseType();
        LocalDateTime phaseOpenAt = phaseType == PhaseType.AUCTION ? popup.getAuctionOpenAt() : popup.getDrawOpenAt();
        LocalDateTime phaseCloseAt = phaseType == PhaseType.AUCTION ? popup.getAuctionCloseAt() : popup.getDrawCloseAt();

        return new PopupCardDto(
                popup.getId(),
                popup.getTitle(),
                banner.getSupportingText(),
                popup.getSubText(),
                popup.getCaption(),
                popup.getThumbnailUrl(),
                false,
                new PopupCardDto.StatsDto(popup.getLikeCount(), popup.getViewCount()),
                null,
                new PopupCardDto.PhaseDto(
                        phaseType,
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
