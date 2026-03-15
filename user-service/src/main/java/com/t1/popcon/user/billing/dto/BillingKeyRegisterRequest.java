package com.t1.popcon.user.billing.dto;

import jakarta.validation.constraints.NotBlank;

public record BillingKeyRegisterRequest(
	@NotBlank(message = "빌링키 식별자는 필수입니다.")
	String customerUid // 포트원 SDK에서 발급받은 식별자
) {}