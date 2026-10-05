package com.lexho.user_service.website.dto;

import jakarta.validation.constraints.NotNull;

public class WebsiteStatusRequest {
    @NotNull
    private Boolean active;

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
