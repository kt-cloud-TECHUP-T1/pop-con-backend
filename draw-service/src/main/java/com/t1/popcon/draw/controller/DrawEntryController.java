package com.t1.popcon.draw.controller;

import com.t1.popcon.common.auth.domain.AuthUser;
import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.draw.dto.request.DrawEntryRequest;
import com.t1.popcon.draw.dto.response.DrawResultConfirmResponse;
import com.t1.popcon.draw.service.DrawEntryService;
import com.t1.popcon.draw.service.DrawResultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/draws")
@RequiredArgsConstructor
public class DrawEntryController {

    private final DrawEntryService drawEntryService;
    private final DrawResultService drawResultService;

    @PostMapping("/{drawId}/options/{optionId}/entries")
    public ApiResponse<Void> applyForDraw(
        @PathVariable Long drawId,
        @PathVariable Long optionId,
        @Valid @RequestBody DrawEntryRequest request,
        @AuthenticationPrincipal AuthUser authUser
    ) {
        drawEntryService.applyForDraw(authUser.id(), drawId, optionId, request);
        return ApiResponse.ok("드로우 응모가 완료되었습니다.");
    }

    @PostMapping("/entries/{entryId}/confirm-result")
    public ApiResponse<DrawResultConfirmResponse> confirmResult(
        @PathVariable("entryId") Long entryId,
        @AuthenticationPrincipal AuthUser authUser
    ) {
        DrawResultConfirmResponse response = drawResultService.confirmResult(authUser.id(), entryId);
        return ApiResponse.ok("드로우 결과 확인 및 티켓 발급에 성공했습니다.", response);
    }
}
