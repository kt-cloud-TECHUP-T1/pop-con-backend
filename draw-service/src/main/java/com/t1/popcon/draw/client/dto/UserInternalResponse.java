package com.t1.popcon.draw.client.dto;

public record UserInternalResponse(
    Long userId,
    String encryptedName,
    String encryptedPhoneNumber
) {
}
