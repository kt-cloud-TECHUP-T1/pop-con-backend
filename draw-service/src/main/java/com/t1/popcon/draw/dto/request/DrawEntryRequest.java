package com.t1.popcon.draw.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record DrawEntryRequest (
	@NotBlank(message = "이름은 필수 입력 사항입니다.")
	String name,

	@NotBlank(message = "전화번호는 필수 입력 사항입니다.")
	@Pattern(regexp = "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$", message = "올바른 전화번호 형식이 아닙니다.")
	String phoneNumber,

	@AssertTrue(message = "개인정보 수집 및 이용에 동의해야 응모가 가능합니다.")
	boolean isPrivacyAgreed,

	@AssertTrue(message = "이용약관에 동의해야 응모가 가능합니다.")
	boolean isTermsAgreed

) {
}