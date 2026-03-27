package com.t1.popcon.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // System
    ERROR_SYSTEM("S001", HttpStatus.INTERNAL_SERVER_ERROR, "시스템 오류가 발생했습니다."),
    METHOD_NOT_ALLOWED("S002", HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),

    // Client
    INVALID_INPUT("C001", HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),

    // Auth
    REGISTER_TOKEN_EXPIRED("A001", HttpStatus.UNAUTHORIZED, "회원가입 세션이 만료되었습니다. 다시 가입 절차를 진행해주세요."),
    INVALID_TOKEN("A002", HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED("A003", HttpStatus.UNAUTHORIZED, "인증이 만료되었습니다. 다시 로그인해주세요."),
    ACCESS_DENIED("A004", HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // OAuth
    INVALID_PROVIDER("OA001", HttpStatus.BAD_REQUEST, "지원하지 않는 소셜 로그인 provider 입니다."),
    OAUTH_INVALID_STATE("OA002", HttpStatus.UNAUTHORIZED, "유효하지 않거나 만료된 state 입니다."),
    OAUTH_TOKEN_EXCHANGE_FAILED("OA003", HttpStatus.BAD_GATEWAY, "소셜 로그인 토큰 발급에 실패했습니다."),
    OAUTH_USERINFO_FAILED("OA004", HttpStatus.BAD_GATEWAY, "소셜 로그인 사용자 정보 조회에 실패했습니다."),

    // Identity Verification
    IDENTITY_VERIFICATION_FETCH_FAILED("I001", HttpStatus.BAD_GATEWAY, "본인인증 정보 조회에 실패했습니다."),
    IDENTITY_VERIFICATION_FAILED("I002", HttpStatus.UNAUTHORIZED, "본인인증 검증에 실패했습니다."),

    // Join
    AGE_RESTRICTED("J001", HttpStatus.FORBIDDEN, "만 14세 미만은 가입이 제한됩니다."),
    ALREADY_SIGNED_UP("J002", HttpStatus.CONFLICT, "이미 약관 동의 및 회원가입이 완료된 회원입니다."),

    // User
    USER_NOT_FOUND("U001", HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
    SOCIAL_INFO_MISSING("U002", HttpStatus.BAD_REQUEST, "가입 세션의 소셜 정보가 누락되었습니다."),

    // Payment
    PAYMENT_FETCH_FAILED("P001", HttpStatus.BAD_GATEWAY, "결제 수단 정보 조회에 실패했습니다."),
    BILLING_KEY_NOT_FOUND("P002", HttpStatus.NOT_FOUND, "등록된 결제 수단이 없습니다."),
    PAYMENT_EXECUTION_FAILED("P003", HttpStatus.PAYMENT_REQUIRED, "결제 승인에 실패했습니다."),
    ALREADY_PAID("P004", HttpStatus.CONFLICT, "이미 결제가 완료된 주문입니다."),
    PAYMENT_CANCEL_FAILED("P005", HttpStatus.BAD_GATEWAY, "결제 취소 요청에 실패했습니다."),

    // Popup
    POPUP_NOT_FOUND("PO001", HttpStatus.NOT_FOUND, "존재하지 않는 팝업스토어입니다."),

    // Auction
    AUCTION_NOT_FOUND("AU001", HttpStatus.NOT_FOUND, "존재하지 않는 경매입니다."),
    AUCTION_NOT_OPEN("AU002", HttpStatus.BAD_REQUEST, "현재 진행 중인 경매가 아닙니다."),
    AUCTION_ALREADY_CLOSED("AU003", HttpStatus.BAD_REQUEST, "이미 종료된 경매입니다."),
    AUCTION_ALREADY_SOLD_OUT("AU004", HttpStatus.CONFLICT, "이미 낙찰이 완료된 경매입니다."),
    AUCTION_STREAM_SUBSCRIBE_FAILED("AU005", HttpStatus.INTERNAL_SERVER_ERROR, "경매 실시간 구독 연결에 실패했습니다."),
    AUCTION_OPTION_NOT_FOUND("AU006", HttpStatus.NOT_FOUND, "존재하지 않는 경매 옵션입니다."),
    AUCTION_OPTION_SOLD_OUT("AU007", HttpStatus.CONFLICT, "해당 회차는 매진되었습니다."),
    AUCTION_OPTION_STOCK_INVALID("AU008", HttpStatus.BAD_REQUEST, "경매 옵션 재고 값이 올바르지 않습니다."),
    AUCTION_PRICE_MISMATCH("AU009", HttpStatus.BAD_REQUEST, "요청하신 입찰 가격이 현재 경매가와 일치하지 않습니다."),

    // Draw
    DRAW_NOT_FOUND("D001", HttpStatus.NOT_FOUND, "존재하지 않는 드로우입니다."),
    DRAW_NOT_OPEN("D002", HttpStatus.BAD_REQUEST, "현재 진행 중인 드로우가 아닙니다."),
    DRAW_ALREADY_CLOSED("D003", HttpStatus.BAD_REQUEST, "이미 종료된 드로우입니다."),
    DRAW_OPTION_NOT_FOUND("D004", HttpStatus.NOT_FOUND, "존재하지 않는 드로우 옵션입니다."),

    // Encryption
    ENCRYPTION_FAILED("E001", HttpStatus.INTERNAL_SERVER_ERROR, "데이터 암호화에 실패했습니다."),
    DECRYPTION_FAILED("E002", HttpStatus.INTERNAL_SERVER_ERROR, "데이터 복호화에 실패했습니다.");


    private final String code;
    private final HttpStatus status;
    private final String message;
}
