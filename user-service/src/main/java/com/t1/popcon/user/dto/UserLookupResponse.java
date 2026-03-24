package com.t1.popcon.user.dto;

public record UserLookupResponse(
        boolean exists,
        Long userId
) {
    public static UserLookupResponse found(Long userId) {
        return new UserLookupResponse(true, userId);
    }

    public static UserLookupResponse notFound() {
        return new UserLookupResponse(false, null);
    }
}