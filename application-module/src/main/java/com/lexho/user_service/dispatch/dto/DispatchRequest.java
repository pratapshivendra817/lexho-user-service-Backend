package com.lexho.user_service.dispatch.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class DispatchRequest {
    @NotNull(message = "addressId is required")
    public Long addressId;
    @NotNull(message = "serviceId is required")
    public Long serviceId;
    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be >= 1")
    public Integer quantity;
}

