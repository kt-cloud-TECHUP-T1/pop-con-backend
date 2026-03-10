package com.t1.popcon.common.auth.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TokenType {
	ACCESS,
	REFRESH
}
