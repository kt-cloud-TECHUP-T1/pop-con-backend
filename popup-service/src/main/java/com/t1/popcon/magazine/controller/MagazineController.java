package com.t1.popcon.magazine.controller;

import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.magazine.dto.section.MagazineSectionResponse;
import com.t1.popcon.magazine.service.MagazineService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
public class MagazineController {

    private final MagazineService magazineService;

    @GetMapping("/magazines")
    public ApiResponse<MagazineSectionResponse> getMagazines(
            @RequestParam(defaultValue = "3") @Min(value = 1, message = "limit는 1 이상이어야 합니다.") int limit
    ) {
        MagazineSectionResponse response = magazineService.getTopMagazines(limit);
        return ApiResponse.ok("매거진 섹션 조회 성공", response);
    }
}
