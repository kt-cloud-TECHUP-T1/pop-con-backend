package com.t1.popcon.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public class AuthRequest {

	public record Signup(
		@Valid
		@NotNull(message = "약관 동의 정보가 필요합니다.")
		Agreements agreements
	) {
	}

	public record Agreements(
		@NotNull(message = "필수 약관 항목이 누락되었습니다.")
		@AssertTrue(message = "필수 동의 항목입니다.")
		Boolean isPrivacyPolicyAgreed,

		@NotNull(message = "필수 약관 항목이 누락되었습니다.")
		@AssertTrue(message = "필수 동의 항목입니다.")
		Boolean isIdentifierPolicyAgreed,

		@NotNull(message = "필수 약관 항목이 누락되었습니다.")
		@AssertTrue(message = "필수 동의 항목입니다.")
		Boolean isServicePolicyAgreed,

		@NotNull(message = "선택 약관 항목이 누락되었습니다.")
		Boolean isMarketingAgreed
	) {
	}
}