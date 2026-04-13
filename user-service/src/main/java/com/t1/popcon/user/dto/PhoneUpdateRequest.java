package com.t1.popcon.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 휴대폰 번호 변경 내부 요청 DTO
 */
public record PhoneUpdateRequest(
        @NotBlank(message = "암호화된 휴대폰 번호가 필요합니다.")
        @Size(max = 255, message = "암호화된 휴대폰 번호가 너무 깁니다.")
        String encryptedPhone,

        @NotBlank(message = "전화번호 해시가 필요합니다.")
        @Size(max = 64)
        String phoneHash
) {
}
