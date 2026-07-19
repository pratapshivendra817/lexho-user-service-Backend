package com.lexho.user_service.exception;

import org.springframework.http.HttpStatus;

public class CodedValidationException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus status;

    public CodedValidationException(String errorCode, String message) {
        this(errorCode, message, HttpStatus.BAD_REQUEST);
    }

    public CodedValidationException(String errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status == null ? HttpStatus.BAD_REQUEST : status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
