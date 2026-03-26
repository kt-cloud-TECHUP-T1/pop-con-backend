package com.t1.popcon.common.exception;

import com.t1.popcon.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * CustomException 처리
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<?>> handleCustom(CustomException e) {
        ErrorCode ec = e.getErrorCode();

        log.error("Business exception",
                kv("logType",ec.name()),
                kv("errorCode", ec.getCode())
        );

        if (e.getData() != null) {
            return ResponseEntity.status(ec.getStatus())
                .body(ApiResponse.fail(ec, e.getData()));
        }

        return ResponseEntity.status(ec.getStatus())
            .body(ApiResponse.fail(ec));
    }

    /**
     * DTO 검증 실패 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            String key = fe.getField();
            String msg = fe.getDefaultMessage() == null ? "입력값이 올바르지 않습니다." : fe.getDefaultMessage();
            fieldErrors.putIfAbsent(key, msg);
        }

        ErrorCode ec = ErrorCode.INVALID_INPUT;

        log.warn("Validation error",
                kv("logType", ec.name()),
                kv("errorCode", ec.getCode()),
                kv("fieldErrors", fieldErrors)
        );

        return ResponseEntity.status(ec.getStatus())
            .body(ApiResponse.fail(ec, fieldErrors));
    }

    /**
     * 파라미터 타입 불일치 오류 처리
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<?>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        ErrorCode ec = ErrorCode.INVALID_INPUT;

        log.warn("Type mismatch",
                kv("logType", ec.name()),
                kv("errorCode", ec.getCode())
        );

        return ResponseEntity.status(ec.getStatus())
            .body(ApiResponse.fail(ec));
    }

    /**
     * 파라미터 검증 실패 처리
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleConstraintViolation(ConstraintViolationException e) {
        ErrorCode ec = ErrorCode.INVALID_INPUT;

        log.warn("Constraint violation",
                kv("logType", ec.name()),
                kv("errorCode", ec.getCode())
        );

        return ResponseEntity.status(ec.getStatus())
            .body(ApiResponse.fail(ec));
    }

    /**
     * 기타 서버 오류 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        ErrorCode ec = ErrorCode.ERROR_SYSTEM;

        log.error("Unhandled exception",
                kv("logType", ec.name()),
                kv("errorCode", ec.getCode()),
                e
        );

        return ResponseEntity.status(ec.getStatus())
            .body(ApiResponse.fail(ec));
    }
}
