package com.t1.popcon.auth.dto;

import jakarta.validation.constraints.NotNull;

public class IdentityRequest {

    public record Complete(
        @NotNull(message = "본인인증 식별자가 필요합니다.")
        String identityVerificationId
    ) {}
}