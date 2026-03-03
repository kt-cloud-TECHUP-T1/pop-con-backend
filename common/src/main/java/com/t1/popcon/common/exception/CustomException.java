package com.t1.popcon.common.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object data;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.data = null;
    }

    public CustomException(ErrorCode errorCode, String overrideMessage) {
        super(overrideMessage);
        this.errorCode = errorCode;
        this.data = null;
    }

    public CustomException(ErrorCode errorCode, Object data) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.data = data;
    }

    public CustomException(ErrorCode errorCode, String overrideMessage, Object data) {
        super(overrideMessage);
        this.errorCode = errorCode;
        this.data = data;
    }
}