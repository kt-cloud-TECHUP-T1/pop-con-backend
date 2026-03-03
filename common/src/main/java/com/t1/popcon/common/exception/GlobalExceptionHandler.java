package com.t1.popcon.common.exception;

import com.t1.popcon.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
        return ResponseEntity.status(ec.getStatus())
            .body(ApiResponse.fail(ec, fieldErrors));
    }

    /**
     * 파라미터 검증 실패 처리
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleConstraintViolation(ConstraintViolationException e) {
        ErrorCode ec = ErrorCode.INVALID_INPUT;
        return ResponseEntity.status(ec.getStatus())
            .body(ApiResponse.fail(ec));
    }

    /**
     * 기타 서버 오류 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("Unhandled exception", e);

        ErrorCode ec = ErrorCode.ERROR_SYSTEM;
        return ResponseEntity.status(ec.getStatus())
            .body(ApiResponse.fail(ec));
    }
}
