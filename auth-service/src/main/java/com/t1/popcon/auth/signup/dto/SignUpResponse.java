package com.t1.popcon.auth.signup.dto;

import java.time.LocalDateTime;

public class SignUpResponse {

	public record Signup(
		Long userId,
		String name,
		String accessToken,
		LocalDateTime createdAt
	) {
	}
}
