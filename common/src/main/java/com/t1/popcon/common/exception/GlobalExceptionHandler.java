package com.t1.popcon.common.exception;

import com.t1.popcon.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.Map;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // CustomException 처리
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<?>> handleCustom(CustomException e, HttpServletRequest request) {
        ErrorCode ec = e.getErrorCode();

        log.error("Business exception",
                ArrayUtils.addAll(
                        baseLog(ec),
                        kv("query", sanitizeQuery(request.getQueryString()))
                )
        );

        // overrideMessage가 있으면 우선 사용, 없으면 ErrorCode 기본 메시지 사용
        String message = e.getMessage() != null ? e.getMessage() : ec.getMessage();

        if (e.getData() != null) {
            return ResponseEntity.status(ec.getStatus())
                    .body(ApiResponse.fail(ec.getCode(), message, e.getData()));
        }

        return ResponseEntity.status(ec.getStatus())
                .body(ApiResponse.fail(ec.getCode(), message));
    }

    // DTO 검증 실패 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            String key = fe.getField();
            String msg = fe.getDefaultMessage() == null ? "입력값이 올바르지 않습니다." : fe.getDefaultMessage();
            fieldErrors.putIfAbsent(key, msg);
        }

        ErrorCode ec = ErrorCode.INVALID_INPUT;

        log.warn("Validation error",
                ArrayUtils.addAll(
                        baseLog(ec),
                        kv("fieldErrors", fieldErrors)
                )
        );

        return ResponseEntity.status(ec.getStatus())
                .body(ApiResponse.fail(ec, fieldErrors));
    }

    // 파라미터 타입 불일치 처리
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<?>> handleTypeMismatch(
            MethodArgumentTypeMismatchException e,
            HttpServletRequest request
    ) {
        ErrorCode ec = ErrorCode.INVALID_INPUT;

        log.warn("Type mismatch", baseLog(ec));

        return ResponseEntity.status(ec.getStatus())
                .body(ApiResponse.fail(ec));
    }

    // 파라미터 검증 실패 처리
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleConstraintViolation(
            ConstraintViolationException e,
            HttpServletRequest request
    ) {
        ErrorCode ec = ErrorCode.INVALID_INPUT;

        log.warn("Constraint violation", baseLog(ec));
        return ResponseEntity.status(ec.getStatus())
                .body(ApiResponse.fail(ec));
    }

    // 허용되지 않는 HTTP 메서드 처리
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException e,
            HttpServletRequest request
    ) {
        ErrorCode ec = ErrorCode.METHOD_NOT_ALLOWED;

        log.warn("Method not supported",
                ArrayUtils.addAll(
                        baseLog(ec),
                        kv("allowedMethods", e.getSupportedMethods())
                )
        );

        String allowMethods = e.getSupportedMethods() != null
                ? String.join(", ", e.getSupportedMethods())
                : "";

        return ResponseEntity.status(ec.getStatus())
                .header("Allow", allowMethods)
                .body(ApiResponse.fail(ec));
    }

    // 파일 크기 초과 처리 (Spring multipart 제한)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<?>> handleMaxUploadSize(
            MaxUploadSizeExceededException e,
            HttpServletRequest request
    ) {
        ErrorCode ec = ErrorCode.FILE_SIZE_EXCEEDED;

        log.warn("File size exceeded", baseLog(ec));

        return ResponseEntity.status(ec.getStatus())
                .body(ApiResponse.fail(ec));
    }

    // 비동기 요청 타임아웃 처리
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public void handleAsyncRequestTimeout(
            AsyncRequestTimeoutException e,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        log.warn("Async request timeout",
                ArrayUtils.addAll(
                        baseLog(ErrorCode.ERROR_SYSTEM),
                        kv("uri", request.getRequestURI())
                )
        );

        if (!response.isCommitted()) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }
    }

    // 기타 서버 오류 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e, HttpServletRequest request) {
        ErrorCode ec = ErrorCode.ERROR_SYSTEM;

        log.error("Unhandled exception",
                ArrayUtils.addAll(baseLog(ec), e)
        );

        return ResponseEntity.status(ec.getStatus())
                .body(ApiResponse.fail(ec));
    }

    private Object[] baseLog(ErrorCode ec) {
        return new Object[]{
                kv("logType", ec.name()),
                kv("errorCode", ec.getCode()),
        };
    }

    private String sanitizeQuery(String query) {
        return query == null ? null : "[REDACTED]";
    }
}
