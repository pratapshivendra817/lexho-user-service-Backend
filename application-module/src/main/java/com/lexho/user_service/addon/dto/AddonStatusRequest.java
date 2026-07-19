package com.lexho.user_service.addon.dto;

import jakarta.validation.constraints.NotNull;

public class AddonStatusRequest {
    @NotNull(message = "active is required")
    private Boolean active;

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
