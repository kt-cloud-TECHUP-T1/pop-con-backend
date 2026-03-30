package com.t1.popcon.queue.controller;

import com.t1.popcon.common.auth.domain.AuthUser;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.queue.common.domain.PhaseType;
import com.t1.popcon.queue.dto.response.QueueEntryResponse;
import com.t1.popcon.queue.dto.response.QueueStatusResponse;
import com.t1.popcon.queue.service.QueueEntryService;
import com.t1.popcon.queue.service.QueuePollingService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 대기열 API 컨트롤러
 * - POST /queues/draws/{drawId} : 드로우 대기열 진입 (JWT 필수)
 * - POST /queues/auctions/{auctionId} : 경매 대기열 진입 (JWT 필수)
 * - GET /queues/status : 상태 폴링 (X-Queue-Token 필수)
 * - DELETE /queues : 자진 이탈 (X-Queue-Token 필수)
 */
@Validated
@RestController
@RequestMapping("/queues")
@RequiredArgsConstructor
public class QueueController {

    private static final String QUEUE_TOKEN_HEADER = "X-Queue-Token";

    private final QueueEntryService entryService;
    private final QueuePollingService pollingService;

    /** 드로우 대기열 진입 */
    @PostMapping("/draws/{drawId}")
    public ApiResponse<QueueEntryResponse> enterDraw(
            @PathVariable @Positive Long drawId,
            @AuthenticationPrincipal AuthUser authUser) {
        long userId = resolveUserId(authUser);
        QueueEntryResponse response = entryService.enter(PhaseType.DRAW, drawId, userId);
        return ApiResponse.ok("대기열 진입에 성공했습니다.", response);
    }

    /** 경매 대기열 진입 */
    @PostMapping("/auctions/{auctionId}")
    public ApiResponse<QueueEntryResponse> enterAuction(
            @PathVariable @Positive Long auctionId,
            @AuthenticationPrincipal AuthUser authUser) {
        long userId = resolveUserId(authUser);
        QueueEntryResponse response = entryService.enter(PhaseType.AUCTION, auctionId, userId);
        return ApiResponse.ok("대기열 진입에 성공했습니다.", response);
    }

    /** 대기열 상태 폴링 */
    @GetMapping("/status")
    public ApiResponse<QueueStatusResponse> getStatus(
            @RequestHeader(value = QUEUE_TOKEN_HEADER, required = false) String queueToken) {
        validateQueueToken(queueToken);
        QueueStatusResponse response = pollingService.getStatus(queueToken);
        return ApiResponse.ok(response);
    }

    /** 대기열 자진 이탈 */
    @DeleteMapping
    public ApiResponse<Void> leave(
            @RequestHeader(value = QUEUE_TOKEN_HEADER, required = false) String queueToken) {
        validateQueueToken(queueToken);
        pollingService.leave(queueToken);
        return ApiResponse.ok("대기열에서 이탈하였습니다.");
    }

    // ── 내부 유틸 ──────────────────────────────────────────────

    /** JWT Principal에서 userId 추출 — 인증 실패 시 Spring Security가 차단하지만 방어 검증 */
    private long resolveUserId(AuthUser authUser) {
        if (authUser == null) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        return authUser.id();
    }

    /** queueToken blank 검증 */
    private void validateQueueToken(String queueToken) {
        if (queueToken == null || queueToken.isBlank()) {
            throw new CustomException(ErrorCode.QUEUE_TOKEN_MISSING);
        }
    }
}
