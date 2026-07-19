package com.lexho.user_service.subscription.exception;

import java.util.List;

public class SubscriptionValidationException extends RuntimeException {

    private final List<String> errors;

    public SubscriptionValidationException(String message) {
        super(message);
        this.errors = List.of(message);
    }

    public SubscriptionValidationException(List<String> errors) {
        super("Validation failed");
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
