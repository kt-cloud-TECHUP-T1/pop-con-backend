package com.t1.popcon.common.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 포트원 V2 결제 요청 응답 DTO (실제 API 응답 구조 반영)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOnePaymentResponse(
    Payment payment
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Payment(
        String pgTxId,         // PG사 거래 번호
        String paidAt          // 결제 완료 일시 (결제 성공 시 존재)
    ) {}

    // 편의를 위한 헬퍼 메서드들
    public boolean isPaid() {
        return payment != null && payment.paidAt() != null;
    }

    public String getPgTxId() {
        return payment != null ? payment.pgTxId() : null;
    }
}