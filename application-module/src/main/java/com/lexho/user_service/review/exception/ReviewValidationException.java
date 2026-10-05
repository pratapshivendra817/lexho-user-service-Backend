package com.lexho.user_service.review.exception;

public class ReviewValidationException extends RuntimeException {
    private final String code;

    public ReviewValidationException(String message) {
        this("INVALID_REVIEW", message);
    }

    public ReviewValidationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
