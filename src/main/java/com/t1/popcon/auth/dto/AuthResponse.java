package com.t1.popcon.auth.dto;

import java.time.LocalDateTime;

public class AuthResponse {

	public record Signup(
		Long userId,
		String name,
		String accessToken,
		String refreshToken,
		LocalDateTime joinedAt
	) {

		// 임시 팩토리 메서드 (User 엔티티 완성 전까지 사용하는 가짜 데이터용)
		public static Signup mockOf() {
			return new Signup(
				999L,
				"임시유저",
				"mock_access_token_123",
				"mock_refresh_token_456",
				LocalDateTime.now()
			);
		}

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