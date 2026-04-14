package com.t1.popcon.user.dto;

public record UserInternalResponse(
    Long userId,
    String encryptedName,
    String encryptedPhoneNumber,
    String ciHash
) {
}
