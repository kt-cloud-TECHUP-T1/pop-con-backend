package com.t1.popcon.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.t1.popcon.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

    private static final String SUCCESS = "SUCCESS";
    private static final String DEFAULT_SUCCESS_MESSAGE = "성공하였습니다.";

    private final String code;
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T data;

    // ====== 성공 응답 ======
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(SUCCESS, message, data);
    }

    public static ApiResponse<Void> ok(String message) {
        return new ApiResponse<>(SUCCESS, message, null);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(SUCCESS, DEFAULT_SUCCESS_MESSAGE, data);
    }

    // ====== 실패 응답 ======
    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public static <T> ApiResponse<T> fail(String code, String message, T data) {
        return new ApiResponse<>(code, message, data);
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, T data) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), data);
    }
}