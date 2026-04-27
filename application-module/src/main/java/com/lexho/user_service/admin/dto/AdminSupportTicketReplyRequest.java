package com.lexho.user_service.admin.dto;

import jakarta.validation.constraints.NotBlank;

public class AdminSupportTicketReplyRequest {

    @NotBlank
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
