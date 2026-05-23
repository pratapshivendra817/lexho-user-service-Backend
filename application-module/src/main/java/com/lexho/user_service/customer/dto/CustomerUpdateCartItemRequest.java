package com.lexho.user_service.customer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CustomerUpdateCartItemRequest {
    @NotNull
    @Positive
    private Long itemId;

    @NotNull
    @Positive
    private Integer quantity;
}
