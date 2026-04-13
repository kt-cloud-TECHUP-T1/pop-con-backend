package com.t1.popcon.auth.client.user.dto;

/**
 * user-service의 PATCH /internal/users/{userId}/phone 요청 DTO
 */
public record PhoneUpdateRequest(
        String encryptedPhone,
        String phoneHash
) {
}
