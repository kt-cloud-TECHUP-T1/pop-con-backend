package com.t1.popcon.user.dto;

public record UserCreateResponse(
	Long id,
	String name,
	String email
) {
	public static UserCreateResponse from(com.t1.popcon.user.domain.User user) {
		return new UserCreateResponse(
			user.getId(),
			user.getEncryptedName(),
			user.getEmail()
		);
	}
}
