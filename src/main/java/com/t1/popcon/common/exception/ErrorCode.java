package com.t1.popcon.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Auth
    INVALID_PROVIDER("A001", HttpStatus.BAD_REQUEST, "지원하지 않는 소셜 로그인 provider 입니다."),
    OAUTH_INVALID_STATE("A002", HttpStatus.UNAUTHORIZED, "유효하지 않거나 만료된 state 입니다."),
    OAUTH_TOKEN_EXCHANGE_FAILED("A003", HttpStatus.BAD_GATEWAY, "소셜 로그인 토큰 발급에 실패했습니다."),
    OAUTH_USERINFO_FAILED("A004", HttpStatus.BAD_GATEWAY, "소셜 로그인 사용자 정보 조회에 실패했습니다."),

    // Client
    INVALID_INPUT("C001", HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),

    // System
    ERROR_SYSTEM("S001", HttpStatus.INTERNAL_SERVER_ERROR, "처리 중 오류가 발생했습니다. 다시 시도해주세요."),

    // User
    USER_NOT_FOUND("U001", HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
    SOCIAL_INFO_MISSING("U002", HttpStatus.BAD_REQUEST, "가입 세션의 소셜 정보가 누락되었습니다.");

    // Join


    private final String code;
    private final HttpStatus status;
    private final String message;
}