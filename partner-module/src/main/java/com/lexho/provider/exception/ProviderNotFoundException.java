package com.lexho.partner.exception;

public class ProviderNotFoundException extends RuntimeException {
    public ProviderNotFoundException(Long id) {
        super("Partner not found for id: " + id);
    }
}

