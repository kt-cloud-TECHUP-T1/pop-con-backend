package com.t1.popcon.queue.controller;

import com.t1.popcon.common.auth.domain.AuthUser;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.queue.dto.request.VqaUserSubmitRequest;
import com.t1.popcon.queue.dto.response.VqaStartResponse;
import com.t1.popcon.queue.dto.response.VqaSubmitResult;
import com.t1.popcon.queue.dto.vqa.VqaNextQuestionResponse;
import com.t1.popcon.queue.service.VqaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/queues/vqa")
@RequiredArgsConstructor
public class VqaController {

    private static final String QUEUE_TOKEN_HEADER = "X-Queue-Token";
    private final VqaService vqaService;

    /** 보안 퀴즈 시작 및 면제 판단 */
    @PostMapping("/start")
    public ApiResponse<VqaStartResponse> start(
            @RequestHeader(value = QUEUE_TOKEN_HEADER, required = false) String queueToken,
            @AuthenticationPrincipal AuthUser authUser) {
        validateQueueToken(queueToken);
        VqaStartResponse response = vqaService.start(queueToken, authUser.id());
        return ApiResponse.ok(response);
    }

    /** 다음 문제 정보 조회 */
    @GetMapping("/next")
    public ApiResponse<VqaNextQuestionResponse> getNextQuestion(
            @RequestParam String sessionId,
            @AuthenticationPrincipal AuthUser authUser) {
        VqaNextQuestionResponse response = vqaService.getNextQuestion(sessionId, authUser.id());
        return ApiResponse.ok(response);
    }

    /** 답변 제출 및 결과 확인 */
    @PostMapping("/submit")
    public ApiResponse<VqaSubmitResult> submit(
            @Valid @RequestBody VqaUserSubmitRequest request,
            @AuthenticationPrincipal AuthUser authUser) {
        VqaSubmitResult result = vqaService.submit(
            request.vqaSessionId(),
            request.videoId(),
            request.questionId(),
            request.userAnswer(),
            request.totalTime(),
            authUser.id()
        );
        return ApiResponse.ok(result);
    }

    private void validateQueueToken(String queueToken) {
        if (queueToken == null || queueToken.isBlank()) {
            throw new CustomException(ErrorCode.QUEUE_TOKEN_MISSING);
        }
    }
}
