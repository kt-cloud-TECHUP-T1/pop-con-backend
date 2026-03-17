package com.t1.popcon.draw.controller;

import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.draw.dto.response.DrawDetailResponse;
import com.t1.popcon.draw.service.DrawService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/draws")
@RequiredArgsConstructor
public class DrawController {

    private final DrawService drawService;

    @GetMapping("/{drawId}")
    public ApiResponse<DrawDetailResponse> getDrawDetail(@PathVariable Long drawId) {
        return ApiResponse.ok(
                "드로우 상세 조회를 성공했습니다.",
                drawService.getDrawDetail(drawId)
        );
    }
}