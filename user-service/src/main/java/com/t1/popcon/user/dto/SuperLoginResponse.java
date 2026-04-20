package com.t1.popcon.user.dto;

public record SuperLoginResponse(
    Long userId,
    String accessToken,
    long expiresIn
) {
}
