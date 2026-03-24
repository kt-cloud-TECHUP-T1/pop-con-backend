package com.t1.popcon.auth.oauth.client.dto;

public record UserSocialLookupResponse(
        boolean exists,
        Long userId
) {
}