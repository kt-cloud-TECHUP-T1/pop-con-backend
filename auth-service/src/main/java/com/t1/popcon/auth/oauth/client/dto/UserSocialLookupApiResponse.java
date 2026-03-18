package com.t1.popcon.auth.oauth.client.dto;

public record UserSocialLookupApiResponse(
        String code,
        String message,
        UserSocialLookupResponse data
) {
}