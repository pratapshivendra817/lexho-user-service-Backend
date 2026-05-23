package com.lexho.user_service.dispatch.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class PartnerShiftUpdateRequest {
    @NotNull(message = "isOnline is required")
    public Boolean isOnline;
    public String currentPincode;
    public LocalDateTime shiftStart;
    public LocalDateTime shiftEnd;
    @Min(value = 1, message = "maxActiveJobs must be >= 1")
    @Max(value = 20, message = "maxActiveJobs must be <= 20")
    public Integer maxActiveJobs;
}

