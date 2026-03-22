package com.t1.popcon.common.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 포트원 본인인증 응답 DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOneIdentityVerificationResponse(
        String id,
        String status,
        VerifiedCustomer verifiedCustomer
) {

    /**
     * 본인인증 완료 고객 정보
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VerifiedCustomer(
            String ci,          // 연계정보 (CI)
            String name,        // 이름
            String gender,      // 성별
            String birthDate,   // 생년월일 (YYYY-MM-DD)
            String phoneNumber, // 전화번호
            Boolean isForeigner // 외국인 여부
    ) {
    }

    /**
     * 인증 완료 여부 확인
     *
     * @return 인증 상태가 VERIFIED 이면 true
     */
    public boolean isVerified() {
        return "VERIFIED".equals(status);
    }
}
