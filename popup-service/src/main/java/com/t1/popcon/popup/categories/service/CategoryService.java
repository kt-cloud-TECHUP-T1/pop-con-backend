package com.t1.popcon.popup.categories.service;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.popup.categories.dto.CategoryIconDto;
import com.t1.popcon.popup.categories.repository.CategoryRepository;
import com.t1.popcon.popup.detail.entity.Popup;
import com.t1.popcon.popup.dto.card.PopupCardDto;
import com.t1.popcon.popup.dto.card.PopupMapper;
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
public class CategoryService {

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public PopupSectionResponse<CategoryIconDto> getCategories(int limit) {
        // limit 유효성 검사 - 범위 초과 시 필드별 상세 메시지 포함
        if (limit < 1 || limit > 6) {
            Map<String, String> errors = new LinkedHashMap<>();
            errors.put("limit", "limit는 1 이상 6 이하여야 합니다.");
            throw new CustomException(ErrorCode.INVALID_INPUT, errors);
        }

        LocalDateTime now = LocalDateTime.now(KST_ZONE);

        List<CategoryIconDto> items = categoryRepository
                .findActiveCategoriesWithPopup(PageRequest.of(0, limit))
                .stream()
                .map(c -> toIconDto(c.getIconUrl(), c.getIconName(), c.getPopup(), now))
                .toList();

        return PopupSectionResponse.of(SectionKey.CATEGORIES, items);
    }

    private CategoryIconDto toIconDto(String iconUrl, String iconName, Popup popup, LocalDateTime now) {
        PopupMapper.PhaseInfo phaseInfo = PopupMapper.resolvePhase(popup, now);

        return new CategoryIconDto(
                iconUrl,
                iconName,
                popup.getId(),
                new PopupCardDto.PhaseDto(
                        phaseInfo.type(),
                        phaseInfo.status(),
                        phaseInfo.openAt().atZone(KST_ZONE).toOffsetDateTime(),
                        phaseInfo.closeAt().atZone(KST_ZONE).toOffsetDateTime()
                )
        );
    }
}
