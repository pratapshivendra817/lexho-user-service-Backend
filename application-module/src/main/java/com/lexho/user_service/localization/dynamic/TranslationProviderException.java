package com.lexho.user_service.localization.dynamic;

public class TranslationProviderException extends RuntimeException {

    public TranslationProviderException(String message) {
        super(message);
    }

    public TranslationProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
