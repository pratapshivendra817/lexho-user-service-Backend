package com.lexho.user_service.customer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CustomerAddToCartRequest {
    @NotNull(message = "serviceId is required")
    private Long serviceId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;
}
