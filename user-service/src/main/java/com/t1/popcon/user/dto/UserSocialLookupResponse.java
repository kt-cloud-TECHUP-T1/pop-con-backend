package com.t1.popcon.user.dto;

public record UserSocialLookupResponse(
        boolean exists,
        Long userId
) {
    public static UserSocialLookupResponse found(Long userId) {
        return new UserSocialLookupResponse(true, userId);
    }

    public static UserSocialLookupResponse notFound() {
        return new UserSocialLookupResponse(false, null);
    }
}