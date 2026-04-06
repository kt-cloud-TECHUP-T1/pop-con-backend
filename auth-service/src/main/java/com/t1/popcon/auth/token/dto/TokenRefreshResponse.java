package com.t1.popcon.auth.token.dto;

import lombok.Builder;

@Builder
public record TokenRefreshResponse(
	String accessToken,
	Long expiresIn
) {
}
