package com.lexho.provider.exception;

public class ProviderNotFoundException extends RuntimeException {
    public ProviderNotFoundException(Long id) {
        super("Provider not found for id: " + id);
    }
}

