package com.t1.popcon.auction.controller;

import com.t1.popcon.auction.dto.response.AuctionAvailableDateResponse;
import com.t1.popcon.auction.dto.response.AuctionOptionResponse;
import com.t1.popcon.auction.service.AuctionOptionService;
import com.t1.popcon.common.auth.domain.AuthUser;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.common.queue.QuizPassedTokenInfo;
import com.t1.popcon.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static com.t1.popcon.common.queue.QuizPassedTokenFilter.QUIZ_PASSED_TOKEN_INFO_ATTRIBUTE;

@RestController
@RequestMapping("/auctions")
@RequiredArgsConstructor
public class AuctionOptionController {

    private final AuctionOptionService auctionOptionService;

    @GetMapping("/{auctionId}/dates")
    public ApiResponse<List<AuctionAvailableDateResponse>> getAvailableDates(
            @PathVariable Long auctionId,
            @AuthenticationPrincipal AuthUser authUser,
            @RequestAttribute(QUIZ_PASSED_TOKEN_INFO_ATTRIBUTE) QuizPassedTokenInfo tokenInfo
    ) {
        validateQuizToken(authUser, tokenInfo, auctionId);
        List<AuctionAvailableDateResponse> data = auctionOptionService.getAvailableDates(auctionId);
        return ApiResponse.ok("선택 가능한 날짜 목록 조회를 성공했습니다.", data);
    }

    @GetMapping("/{auctionId}/dates/{entryDate}/options")
    public ApiResponse<List<AuctionOptionResponse>> getOptionsByDate(
        @PathVariable Long auctionId,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entryDate,
        @AuthenticationPrincipal AuthUser authUser,
        @RequestAttribute(QUIZ_PASSED_TOKEN_INFO_ATTRIBUTE) QuizPassedTokenInfo tokenInfo
    ) {
        validateQuizToken(authUser, tokenInfo, auctionId);
        List<AuctionOptionResponse> data = auctionOptionService.getOptionsByDate(auctionId, entryDate);
        return ApiResponse.ok("날짜별 입장 시간 목록 조회를 성공했습니다.", data);
    }

    private void validateQuizToken(AuthUser authUser, QuizPassedTokenInfo tokenInfo, Long auctionId) {
        if (authUser == null || tokenInfo == null) {
            throw new CustomException(ErrorCode.QUIZ_PASSED_TOKEN_MISSING);
        }
        if (!authUser.id().equals(tokenInfo.userId())) {
            throw new CustomException(ErrorCode.QUIZ_PASSED_TOKEN_INVALID, "본인의 퀴즈 결과만 사용할 수 있습니다.");
        }
        if (!auctionId.equals(tokenInfo.phaseId()) || !"auction".equals(tokenInfo.phaseType())) {
            throw new CustomException(ErrorCode.QUIZ_PASSED_TOKEN_INVALID, "해당 경매에 유효한 퀴즈 토큰이 아닙니다.");
        }
    }
}