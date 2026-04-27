package com.lexho.user_service.review.exception;

public class ReviewAccessDeniedException extends RuntimeException {

    public ReviewAccessDeniedException(String message) {
        super(message);
    }
}
