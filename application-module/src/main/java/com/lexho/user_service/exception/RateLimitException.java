package com.lexho.user_service.exception;

public class RateLimitException extends TooManyRequestsException {
    public RateLimitException(String message) {
        super(message);
    }
}
