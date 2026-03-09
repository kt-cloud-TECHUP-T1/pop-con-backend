package com.t1.popcon.popup.detail.dto;

import jakarta.validation.constraints.NotNull;

public class PopupDetailRequest {
	public record Detail(
		@NotNull(message = "팝업스토어 아이디는 필수입니다.")
		int popupId
	) {
	}
}
