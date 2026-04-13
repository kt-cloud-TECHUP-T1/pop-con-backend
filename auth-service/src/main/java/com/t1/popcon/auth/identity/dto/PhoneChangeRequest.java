package com.t1.popcon.auth.identity.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 휴대폰 번호 변경 요청 DTO
 * POST /auth/identity/phone-change
 */
public record PhoneChangeRequest(
        @NotBlank(message = "본인인증 식별자가 필요합니다.")
        String identityVerificationId
) {
}
