package com.t1.popcon.auth.client.user.dto;

/**
 * user-service의 GET /internal/users/{userId} 응답 DTO
 */
public record UserInternalResponse(
        Long userId,
        String encryptedName,
        String encryptedPhoneNumber,
        String ciHash
) {
}
