package com.t1.popcon.popup.banners.service;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.popup.banners.entity.Banner;
import com.t1.popcon.popup.banners.repository.BannerRepository;
import com.t1.popcon.popup.detail.entity.Popup;
import com.t1.popcon.popup.dto.card.PopupCardDto;
import com.t1.popcon.popup.dto.card.PopupMapper;
import com.t1.popcon.popup.dto.section.PopupSectionResponse;
import com.t1.popcon.popup.dto.section.SectionKey;
import com.t1.popcon.popup.likes.service.PopupLikeReadService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PopupBannersService {

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    private final BannerRepository bannerRepository;
    private final PopupLikeReadService popupLikeReadService;

    @Transactional(readOnly = true)
    public PopupSectionResponse<PopupCardDto> getBanners(Long userId, int limit) {
        if (limit < 1 || limit > 5) {
            Map<String, Object> errors = new LinkedHashMap<>();
            errors.put("limit", "limit는 1 이상 5 이하여야 합니다.");
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        List<Banner> banners = bannerRepository.findActiveBannersWithPopup(PageRequest.of(0, limit));
        Set<Long> likedPopupIds = popupLikeReadService.getLikedPopupIds(
            userId,
            banners.stream().map(banner -> banner.getPopup().getId()).toList()
        );

        List<PopupCardDto> items = banners.stream()
            .map(banner -> toPopupCardDto(banner, likedPopupIds.contains(banner.getPopup().getId())))
            .toList();

        return new PopupSectionResponse<>(SectionKey.BANNERS, items.size(), items);
    }

    private PopupCardDto toPopupCardDto(Banner banner, boolean liked) {
        Popup popup = banner.getPopup();
        PopupMapper.PhaseInfo phaseInfo = PopupMapper.resolvePhase(popup, LocalDateTime.now(KST_ZONE));

        return new PopupCardDto(
            popup.getId(),
            popup.getTitle(),
            banner.getSupportingText(),
            popup.getSubText(),
            popup.getCaption(),
            popup.getVThumbUrl(),
            liked,
            new PopupCardDto.StatsDto(popup.getLikeCount(), popup.getViewCount()),
            null,
            new PopupCardDto.PhaseDto(
                phaseInfo.type(),
                phaseInfo.status(),
                phaseInfo.openAt().atZone(KST_ZONE).toOffsetDateTime(),
                phaseInfo.closeAt().atZone(KST_ZONE).toOffsetDateTime()
            )
        );
    }
}
