package com.lexho.user_service.review.exception;

public class ReviewNotFoundException extends RuntimeException {
    private final String code;

    public ReviewNotFoundException(Long id) {
        super("Review not found with id: " + id);
        this.code = "BOOKING_NOT_FOUND";
    }

    public ReviewNotFoundException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
