package com.t1.popcon.user.billing.dto;

import jakarta.validation.constraints.NotNull;

public record BillingKeyRegisterRequest(
	@NotNull(message = "빌링키 식별자는 필수입니다.")
	String customerUid // 포트원 SDK에서 발급받은 식별자
) {}