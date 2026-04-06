package com.t1.popcon.auth.client.user.dto;

public record UserLookupResponse(
        boolean exists,
        Long userId
) {
}