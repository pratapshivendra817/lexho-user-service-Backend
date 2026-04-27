package com.lexho.user_service.admin.dto;

import jakarta.validation.constraints.NotBlank;

public class AdminSupportTicketStatusRequest {

    @NotBlank
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
