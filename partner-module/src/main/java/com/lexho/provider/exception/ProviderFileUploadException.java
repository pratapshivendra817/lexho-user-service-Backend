package com.lexho.provider.exception;

public class ProviderFileUploadException extends RuntimeException {
    private final String code;

    public ProviderFileUploadException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
