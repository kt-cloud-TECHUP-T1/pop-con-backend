package com.t1.popcon.auth.token.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(
	@NotBlank(message = "refreshToken이 필요합니다.") String refreshToken
) {}