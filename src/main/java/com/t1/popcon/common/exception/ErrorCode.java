package com.t1.popcon.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // System
    ERROR_SYSTEM("S001", HttpStatus.INTERNAL_SERVER_ERROR, "시스템 오류가 발생했습니다."),

    // OAuth
    INVALID_PROVIDER("OA001", HttpStatus.BAD_REQUEST, "지원하지 않는 소셜 로그인 provider 입니다."),
    OAUTH_INVALID_STATE("OA002", HttpStatus.UNAUTHORIZED, "유효하지 않거나 만료된 state 입니다."),
    OAUTH_TOKEN_EXCHANGE_FAILED("OA003", HttpStatus.BAD_GATEWAY, "소셜 로그인 토큰 발급에 실패했습니다."),
    OAUTH_USERINFO_FAILED("OA004", HttpStatus.BAD_GATEWAY, "소셜 로그인 사용자 정보 조회에 실패했습니다."),

    // Client
    INVALID_INPUT("C001", HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),

    // Auth
    SIGNUP_TOKEN_EXPIRED("A001", HttpStatus.UNAUTHORIZED, "회원가입 세션이 만료되었습니다. 다시 가입 절차를 진행해주세요."),
    INVALID_TOKEN("A002", HttpStatus.UNAUTHORIZED, "인증 정보가 유효하지 않습니다."),
    TOKEN_EXPIRED("A003", HttpStatus.UNAUTHORIZED, "인증이 만료되었습니다. 다시 로그인해주세요."),
    ACCESS_DENIED("A004", HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // Join
    AGE_RESTRICTED("J001", HttpStatus.FORBIDDEN, "만 14세 미만은 가입이 제한됩니다."),
    ALREADY_SIGNED_UP("J002", HttpStatus.CONFLICT, "이미 약관 동의 및 회원가입이 완료된 회원입니다."),

    // User
    USER_NOT_FOUND("U001", HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
    SOCIAL_INFO_MISSING("U002", HttpStatus.BAD_REQUEST, "가입 세션의 소셜 정보가 누락되었습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;
}