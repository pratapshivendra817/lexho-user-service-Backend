package com.lexho.user_service.review.exception;

public class ReviewNotFoundException extends RuntimeException {

    public ReviewNotFoundException(Long id) {
        super("Review not found with id: " + id);
    }
}
