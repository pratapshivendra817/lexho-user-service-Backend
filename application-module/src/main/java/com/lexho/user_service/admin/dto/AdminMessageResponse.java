package com.lexho.user_service.admin.dto;

public class AdminMessageResponse {
    private String message;

    public AdminMessageResponse() {
    }

    public AdminMessageResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
