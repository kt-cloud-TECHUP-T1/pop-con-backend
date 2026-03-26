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
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PopupBannersService {

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    private final BannerRepository bannerRepository;

    @Transactional(readOnly = true)
    public PopupSectionResponse<PopupCardDto> getBanners(int limit) {
        // 유효성 검사 로직
        if (limit < 1 || limit > 5) {
            Map<String, Object> errors = new LinkedHashMap<>();
            errors.put("limit", "limit는 1 이상 5 이하여야 합니다.");
            // CustomException이 Map을 인자로 받지 못하는 경우를 대비해 ErrorCode만 넘기거나 확인 필요
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        List<PopupCardDto> items = bannerRepository.findActiveBannersWithPopup(PageRequest.of(0, limit))
                .stream()
                .map(this::toPopupCardDto)
                .toList();

        return new PopupSectionResponse<>(SectionKey.BANNERS, items.size(), items);
    }

    private PopupCardDto toPopupCardDto(Banner banner) {
        Popup popup = banner.getPopup();
        LocalDateTime now = LocalDateTime.now(KST_ZONE);

        // 페이즈 타입 결정
        PhaseType phaseType = determinePhaseType(popup, now);

        // 결정된 타입에 따른 시간 설정
        LocalDateTime openAt = (phaseType == PhaseType.AUCTION) ? popup.getAuctionOpenAt() : popup.getDrawOpenAt();
        LocalDateTime closeAt = (phaseType == PhaseType.AUCTION) ? popup.getAuctionCloseAt() : popup.getDrawCloseAt();

        // 팝업 카드 DTO 생성 (생성자 파라미터 순서 및 타입 주의)
        return new PopupCardDto(
                popup.getId(),
                popup.getTitle(),
                banner.getSupportingText(),
                popup.getSubText(),
                popup.getCaption(),
                popup.getThumbnailUrl(),
                false, // isLiked 기본값
                new PopupCardDto.StatsDto(popup.getLikeCount(), popup.getViewCount()),
                null,  // location 등 추가 필드
                new PopupCardDto.PhaseDto(
                        phaseType,
                        calculatePhaseStatus(openAt, closeAt, now),
                        openAt.atZone(KST_ZONE).toOffsetDateTime(),
                        closeAt.atZone(KST_ZONE).toOffsetDateTime()
                )
        );
    }

    private PhaseType determinePhaseType(Popup popup, LocalDateTime now) {
        PhaseStatus auctionStatus = calculatePhaseStatus(popup.getAuctionOpenAt(), popup.getAuctionCloseAt(), now);
        PhaseStatus drawStatus = calculatePhaseStatus(popup.getDrawOpenAt(), popup.getDrawCloseAt(), now);

        // 1. 진행 중인 것 우선
        if (auctionStatus == PhaseStatus.OPEN) return PhaseType.AUCTION;
        if (drawStatus == PhaseStatus.OPEN) return PhaseType.DRAW;

        // 2. 오픈 예정인 것 우선
        if (auctionStatus == PhaseStatus.UPCOMING && drawStatus != PhaseStatus.UPCOMING) return PhaseType.AUCTION;
        if (drawStatus == PhaseStatus.UPCOMING && auctionStatus != PhaseStatus.UPCOMING) return PhaseType.DRAW;

        // 3. 둘 다 예정이면 더 빨리 열리는 것
        if (auctionStatus == PhaseStatus.UPCOMING) {
            return popup.getAuctionOpenAt().isBefore(popup.getDrawOpenAt()) ? PhaseType.AUCTION : PhaseType.DRAW;
        }

        // 4. 둘 다 종료면 더 나중에 닫힌 것
        return popup.getAuctionCloseAt().isAfter(popup.getDrawCloseAt()) ? PhaseType.AUCTION : PhaseType.DRAW;
    }

    private PhaseStatus calculatePhaseStatus(LocalDateTime openAt, LocalDateTime closeAt, LocalDateTime now) {
        if (openAt == null || closeAt == null) return PhaseStatus.CLOSED; // null 방어 코드
        if (now.isBefore(openAt)) return PhaseStatus.UPCOMING;
        if (now.isAfter(closeAt)) return PhaseStatus.CLOSED;
        return PhaseStatus.OPEN;
    }
}