package com.lexho.user_service.admin.dto;

import jakarta.validation.constraints.NotNull;

public class AdminServiceStatusRequest {

    @NotNull(message = "Active status is required")
    private Boolean active;

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}

