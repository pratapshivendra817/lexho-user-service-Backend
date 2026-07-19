package com.lexho.user_service.exception;

public class FileUploadException extends RuntimeException {
    private final String code;

    public FileUploadException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
