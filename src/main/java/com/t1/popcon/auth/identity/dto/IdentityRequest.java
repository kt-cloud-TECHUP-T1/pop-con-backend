package com.t1.popcon.auth.identity.dto;

import jakarta.validation.constraints.NotBlank;

public class IdentityRequest {

    public record Complete(
        @NotBlank(message = "본인인증 식별자가 필요합니다.")
        String identityVerificationId
    ) {}
}