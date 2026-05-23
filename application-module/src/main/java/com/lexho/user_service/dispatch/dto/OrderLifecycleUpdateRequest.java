package com.lexho.user_service.dispatch.dto;

import com.lexho.common.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public class OrderLifecycleUpdateRequest {
    @NotNull(message = "status is required")
    public OrderStatus status;
}

