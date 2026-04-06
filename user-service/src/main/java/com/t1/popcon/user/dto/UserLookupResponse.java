package com.t1.popcon.user.dto;

import java.util.Objects;

public record UserLookupResponse(
        boolean exists,
        Long userId
) {
    public static UserLookupResponse found(Long userId) {
	    return new UserLookupResponse(true, Objects.requireNonNull(userId, "userId must not be null"));
    }

    public static UserLookupResponse notFound() {
        return new UserLookupResponse(false, null);
    }
}