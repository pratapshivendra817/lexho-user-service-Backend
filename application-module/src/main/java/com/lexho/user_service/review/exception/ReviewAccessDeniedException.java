package com.lexho.user_service.review.exception;

public class ReviewAccessDeniedException extends RuntimeException {
    private final String code;

    public ReviewAccessDeniedException(String message) {
        super(message);
        this.code = "FORBIDDEN";
    }

    public String getCode() {
        return code;
    }
}
