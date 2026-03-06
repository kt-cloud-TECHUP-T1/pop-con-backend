package com.t1.popcon.auth.signup.dto;

import java.time.LocalDateTime;

public class SignUpResponse {

	public record Signup(
		Long userId,
		String name,
		String accessToken,
		String refreshToken,
		LocalDateTime joinedAt
	) {

		/*

		TODO: 나중에 User 엔티티 완성되면 주석 해제하고 사용

        public static Signup of(User user, String accessToken, String refreshToken) {
            return new Signup(
                user.getId(),
                user.getName(),
                accessToken,
                refreshToken,
                user.getJoinedAt()
            );
        }

  	*/

	}
}