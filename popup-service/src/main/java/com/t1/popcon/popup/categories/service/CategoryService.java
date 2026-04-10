package com.t1.popcon.popup.categories.service;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.popup.categories.dto.CategoryIconDto;
import com.t1.popcon.popup.categories.repository.CategoryRepository;
import com.t1.popcon.popup.dto.section.PopupSectionResponse;
import com.t1.popcon.popup.dto.section.SectionKey;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public PopupSectionResponse<CategoryIconDto> getCategories(int limit) {
        // limit 유효성 검사 - 범위 초과 시 필드별 상세 메시지 포함
        if (limit < 1 || limit > 6) {
            Map<String, String> errors = new LinkedHashMap<>();
            errors.put("limit", "limit는 1 이상 6 이하여야 합니다.");
            throw new CustomException(ErrorCode.INVALID_INPUT, errors);
        }

        List<CategoryIconDto> items = categoryRepository
                .findActiveCategoriesWithPopup(PageRequest.of(0, limit))
                .stream()
                .map(c -> new CategoryIconDto(
                        c.getIconUrl(),
                        c.getIconName(),
                        c.getPopup().getId()
                ))
                .toList();

        return PopupSectionResponse.of(SectionKey.CATEGORIES, items);
    }
}
