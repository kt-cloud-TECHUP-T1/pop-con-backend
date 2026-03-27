package com.t1.popcon.common.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 포트원 V2 결제 취소 응답 DTO (실제 API 응답 구조 반영)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOneCancelResponse(
    Cancellation cancellation
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cancellation(
        String status,         // SUCCEEDED, FAILED, REQUESTED
        String id,             // 취소 고유 ID
        String pgCancellationId, // PG사 취소 번호
        int totalAmount,       // 취소된 총 금액
        String reason,         // 취소 사유
        String cancelledAt     // 취소 일시
    ) {}

    // 편의를 위한 헬퍼 메서드들
    public String status() {
        return cancellation != null ? cancellation.status() : null;
    }

    public String reason() {
        return cancellation != null ? cancellation.reason() : null;
    }

    public boolean isSucceeded() {
        return "SUCCEEDED".equals(status());
    }

    public boolean isRequested() {
        return "REQUESTED".equals(status());
    }

    public boolean isFailed() {
        return "FAILED".equals(status());
    }
}