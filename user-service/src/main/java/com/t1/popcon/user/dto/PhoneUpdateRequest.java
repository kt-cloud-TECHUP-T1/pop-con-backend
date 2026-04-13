package com.t1.popcon.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 휴대폰 번호 변경 내부 요청 DTO
 */
public record PhoneUpdateRequest(
        @NotBlank(message = "암호화된 휴대폰 번호가 필요합니다.")
        String encryptedPhone
) {
}
