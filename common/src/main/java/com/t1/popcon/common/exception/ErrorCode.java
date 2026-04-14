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
    SOCIAL_INFO_MISSING("U002", HttpStatus.BAD_REQUEST, "가입 세션의 소셜 정보가 유실되었습니다."),
    PHONE_ALREADY_IN_USE("U004", HttpStatus.CONFLICT, "이미 사용중인 휴대폰 번호입니다."),
    SAME_PHONE_NUMBER("U005", HttpStatus.CONFLICT, "현재 사용 중인 번호와 동일합니다."),

    // Payment
    PAYMENT_FETCH_FAILED("P001", HttpStatus.BAD_GATEWAY, "결제 수단 정보 조회에 실패했습니다."),
    BILLING_KEY_NOT_FOUND("P002", HttpStatus.NOT_FOUND, "등록된 결제 수단이 없습니다."),
    PAYMENT_EXECUTION_FAILED("P003", HttpStatus.PAYMENT_REQUIRED, "결제 승인에 실패했습니다."),
    ALREADY_PAID("P004", HttpStatus.CONFLICT, "이미 결제가 완료된 주문입니다."),
    PAYMENT_CANCEL_FAILED("P005", HttpStatus.BAD_GATEWAY, "결제 취소 요청에 실패했습니다."),

    // Popup
    POPUP_NOT_FOUND("PO001", HttpStatus.NOT_FOUND, "존재하지 않는 팝업스토어입니다."),

    // Ticket
    TICKET_NOT_FOUND("T001", HttpStatus.NOT_FOUND, "존재하지 않는 티켓입니다."),
    TICKET_NUMBER_ALREADY_ASSIGNED("T002", HttpStatus.CONFLICT, "이미 티켓 번호가 할당되었습니다."),
    TICKET_ALREADY_ISSUED("T003", HttpStatus.CONFLICT, "이미 티켓이 발급되었습니다."),

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
    BID_NOT_FOUND("AU010", HttpStatus.NOT_FOUND, "존재하지 않는 입찰 정보입니다."),
    AUCTION_ALREADY_PARTICIPATED("AU011", HttpStatus.CONFLICT, "이미 해당 경매에 낙찰된 내역이 있습니다."),

    // Draw
    DRAW_NOT_FOUND("D001", HttpStatus.NOT_FOUND, "존재하지 않는 드로우입니다."),
    DRAW_NOT_OPEN("D002", HttpStatus.BAD_REQUEST, "현재 진행 중인 드로우가 아닙니다."),
    DRAW_ALREADY_CLOSED("D003", HttpStatus.BAD_REQUEST, "이미 종료된 드로우입니다."),
    DRAW_OPTION_NOT_FOUND("D004", HttpStatus.NOT_FOUND, "존재하지 않는 드로우 옵션입니다."),
    DRAW_ALREADY_APPLIED("D005", HttpStatus.CONFLICT, "이미 해당 회차에 응모했습니다."),
    DRAW_RESULT_NOT_READY("D006", HttpStatus.BAD_REQUEST, "추첨 결과가 아직 준비되지 않았습니다."),
    DRAW_RESULT_NOT_ANNOUNCED("D007", HttpStatus.BAD_REQUEST, "추첨 결과 발표 전입니다."),
    DRAW_NOT_WINNER("D008", HttpStatus.BAD_REQUEST, "당첨된 응모가 아닙니다."),
    DRAW_ALREADY_PROCESSED("D009", HttpStatus.CONFLICT, "이미 추첨이 완료된 드로우 옵션입니다."),
    DRAW_ENTRY_NOT_FOUND("D010", HttpStatus.NOT_FOUND, "존재하지 않는 드로우 응모입니다."),

    // Encryption
    ENCRYPTION_FAILED("E001", HttpStatus.INTERNAL_SERVER_ERROR, "데이터 암호화에 실패했습니다."),
    DECRYPTION_FAILED("E002", HttpStatus.INTERNAL_SERVER_ERROR, "데이터 복호화에 실패했습니다."),

    // External Service
    EXTERNAL_SERVICE_ERROR("ES001", HttpStatus.BAD_GATEWAY, "외부 서비스 연동에 실패했습니다."),

    // Queue
    QUEUE_BLOCKED("Q001", HttpStatus.FORBIDDEN, "정책 위반으로 접근이 제한되었습니다."),
    QUEUE_TOKEN_MISSING("Q002", HttpStatus.UNAUTHORIZED, "대기열 토큰이 필요합니다."),
    QUEUE_TOKEN_INVALID("Q003", HttpStatus.UNAUTHORIZED, "유효하지 않은 대기열 토큰입니다."),
    QUEUE_NOT_ACTIVE("Q004", HttpStatus.FORBIDDEN, "아직 활성 상태가 아닙니다."),

    // Quiz
    QUIZ_PASSED_TOKEN_MISSING("QZ001", HttpStatus.UNAUTHORIZED, "퀴즈 통과 토큰이 필요합니다."),
    QUIZ_PASSED_TOKEN_INVALID("QZ002", HttpStatus.UNAUTHORIZED, "유효하지 않은 퀴즈 통과 토큰입니다."),
    VQA_SESSION_EXPIRED("QZ003", HttpStatus.NOT_FOUND, "유효하지 않거나 만료된 보안 퀴즈 세션입니다."),
    QUIZ_ATTEMPTS_EXCEEDED("QZ004", HttpStatus.FORBIDDEN, "보안 퀴즈 시도 횟수를 초과했습니다."),
    QUIZ_ALREADY_PASSED("QZ005", HttpStatus.BAD_REQUEST, "이미 보안 퀴즈를 통과했습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;
}
