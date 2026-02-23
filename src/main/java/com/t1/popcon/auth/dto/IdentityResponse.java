package com.t1.popcon.auth.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class IdentityResponse {

    public record ExistingUserComplete(
        boolean isNewUser,
        Long userId,
        String accessToken,
        String refreshToken
    ) {
        public static ExistingUserComplete mockOf() {
            return new ExistingUserComplete(
                false,
                105L,
                "mock_access_token",
                "mock_refresh_token");
        }
    }

    public record NewUserComplete(
        boolean isNewUser,
        String registerToken,
        String nextStep,
        LocalDateTime expiresAt
    ) {
        public static NewUserComplete mockOf() {
            return new NewUserComplete(
                true,
                "reg_" + UUID.randomUUID(),
                "TERMS",
                LocalDateTime.now().plusMinutes(10)
            );
        }
    }
}