package com.lexho.user_service.dispatch.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class PartnerAvailabilityUpdateRequest {
    public Boolean isOnline;
    public Boolean online;
    public Boolean available;
    @Min(value = 1, message = "maxActiveJobs must be >= 1")
    @Max(value = 20, message = "maxActiveJobs must be <= 20")
    public Integer maxActiveJobs;

    public Boolean resolveOnline() {
        if (isOnline != null) {
            return isOnline;
        }
        if (online != null) {
            return online;
        }
        return available;
    }
}
