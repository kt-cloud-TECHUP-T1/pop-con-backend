package com.t1.popcon.popup.categories.controller;

import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.popup.categories.dto.CategoryIconDto;
import com.t1.popcon.popup.categories.service.CategoryService;
import com.t1.popcon.popup.dto.section.PopupSectionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/popups/categories")
public class CategoryController {

    private final CategoryService categoryService;

    // 카테고리 섹션 조회 - 아이콘 이미지, 이름, 연결 팝업 ID 반환
    @GetMapping
    public ResponseEntity<ApiResponse<PopupSectionResponse<CategoryIconDto>>> getCategories(
        @RequestParam(defaultValue = "6") int limit
    ) {
        PopupSectionResponse<CategoryIconDto> data = categoryService.getCategories(limit);
        return ResponseEntity.ok(ApiResponse.ok("카테고리 섹션 조회를 성공했습니다.", data));
    }
}
