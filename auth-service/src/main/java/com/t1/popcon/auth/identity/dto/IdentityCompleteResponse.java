package com.t1.popcon.auth.identity.dto;

public sealed interface IdentityCompleteResponse
		permits IdentityCompleteResponse.ExistingUserComplete,
		IdentityCompleteResponse.NewUserComplete {

	record ExistingUserComplete(
			boolean isNewUser,
			Long userId,
			String accessToken
	) implements IdentityCompleteResponse {
		public static ExistingUserComplete of(Long userId, String accessToken) {
			return new ExistingUserComplete(false, userId, accessToken);
		}
	}

	record NewUserComplete(
			boolean isNewUser,
			String nextStep
	) implements IdentityCompleteResponse {
		public static NewUserComplete terms() {
			return new NewUserComplete(true, "TERMS");
		}
	}
}