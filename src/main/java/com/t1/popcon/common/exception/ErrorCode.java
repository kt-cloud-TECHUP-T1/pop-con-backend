package com.t1.popcon.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Auth

    // Client
    INVALID_INPUT("C001", HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),

    // System
    ERROR_SYSTEM("S001", HttpStatus.INTERNAL_SERVER_ERROR, "처리 중 오류가 발생했습니다. 다시 시도해주세요.");

    private final String code;
    private final HttpStatus status;
    private final String message;
}