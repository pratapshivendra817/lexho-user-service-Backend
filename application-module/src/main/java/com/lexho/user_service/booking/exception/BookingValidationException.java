package com.lexho.user_service.booking.exception;

public class BookingValidationException extends RuntimeException {
    private final String errorCode;

    public BookingValidationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
