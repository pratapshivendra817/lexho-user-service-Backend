package com.lexho.user_service.booking.exception;

public class InstantBookingUnavailableException extends RuntimeException {
    private final String errorCode;

    public InstantBookingUnavailableException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
