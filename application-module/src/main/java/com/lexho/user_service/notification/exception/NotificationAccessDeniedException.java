package com.lexho.user_service.notification.exception;

public class NotificationAccessDeniedException extends RuntimeException {

    public NotificationAccessDeniedException(String message) {
        super(message);
    }
}
