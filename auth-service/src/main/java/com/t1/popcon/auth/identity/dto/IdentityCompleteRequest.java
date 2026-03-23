package com.t1.popcon.auth.identity.dto;

import jakarta.validation.constraints.NotBlank;

public sealed interface IdentityCompleteRequest permits IdentityCompleteRequest.Complete {

	String identityVerificationId();

	record Complete(
			@NotBlank(message = "본인인증 식별자가 필요합니다.")
			String identityVerificationId
	) implements IdentityCompleteRequest {
	}
}