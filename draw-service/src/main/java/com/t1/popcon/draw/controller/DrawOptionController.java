package com.t1.popcon.draw.controller;

import com.t1.popcon.common.auth.domain.AuthUser;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.common.queue.QuizPassedTokenInfo;
import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.draw.dto.response.DrawAvailableDateResponse;
import com.t1.popcon.draw.dto.response.DrawOptionResponse;
import com.t1.popcon.draw.service.DrawOptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static com.t1.popcon.common.queue.QuizPassedTokenFilter.QUIZ_PASSED_TOKEN_INFO_ATTRIBUTE;

@RestController
@RequestMapping("/draws")
@RequiredArgsConstructor
public class DrawOptionController {

    private final DrawOptionService drawOptionService;

    @GetMapping("/{drawId}/dates")
    public ApiResponse<List<DrawAvailableDateResponse>> getAvailableDates(
            @PathVariable Long drawId,
            @AuthenticationPrincipal AuthUser authUser,
            @RequestAttribute(value = QUIZ_PASSED_TOKEN_INFO_ATTRIBUTE, required = false) QuizPassedTokenInfo tokenInfo
    ) {
        validateQuizToken(authUser, tokenInfo, drawId);
        List<DrawAvailableDateResponse> data = drawOptionService.getAvailableDates(drawId);
        return ApiResponse.ok("선택 가능한 날짜 목록 조회를 성공했습니다.", data);
    }

    @GetMapping("/{drawId}/dates/{entryDate}/options")
    public ApiResponse<List<DrawOptionResponse>> getOptionsByDate(
        @PathVariable Long drawId,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entryDate,
        @AuthenticationPrincipal AuthUser authUser,
        @RequestAttribute(value = QUIZ_PASSED_TOKEN_INFO_ATTRIBUTE, required = false) QuizPassedTokenInfo tokenInfo
    ) {
        validateQuizToken(authUser, tokenInfo, drawId);
        List<DrawOptionResponse> data = drawOptionService.getOptionsByDate(drawId, entryDate);
        return ApiResponse.ok("날짜별 입장 시간 목록 조회를 성공했습니다.", data);
    }

    private void validateQuizToken(AuthUser authUser, QuizPassedTokenInfo tokenInfo, Long drawId) {
        if (authUser == null || tokenInfo == null) {
            throw new CustomException(ErrorCode.QUIZ_PASSED_TOKEN_MISSING);
        }
        if (!authUser.id().equals(tokenInfo.userId())) {
            throw new CustomException(ErrorCode.QUIZ_PASSED_TOKEN_INVALID, "본인의 퀴즈 결과만 사용할 수 있습니다.");
        }
        if (!drawId.equals(tokenInfo.phaseId()) || !"draw".equals(tokenInfo.phaseType())) {
            throw new CustomException(ErrorCode.QUIZ_PASSED_TOKEN_INVALID, "해당 드로우에 유효한 퀴즈 토큰이 아닙니다.");
        }
    }
}