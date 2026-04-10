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

import java.util.LinkedHashMap;
import java.util.Map;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<?>> handleCustom(CustomException e, HttpServletRequest request) {
        ErrorCode ec = e.getErrorCode();

        log.error("Business exception",
                ArrayUtils.addAll(
                        baseLog(ec),
                        kv("query", sanitizeQuery(request.getQueryString()))
                )
        );

        if (e.getData() != null) {
            return ResponseEntity.status(ec.getStatus())
                    .body(ApiResponse.fail(ec, e.getData()));
        }

        return ResponseEntity.status(ec.getStatus())
                .body(ApiResponse.fail(ec));
    }

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
