package com.lexho.user_service.customer.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CustomerUpdateCartItemRequest {
    @NotNull
    @Positive
    private Long itemId;

    @Positive
    private Integer quantity;

    private Long familySizeId;
    private String familySize;
    private String familySizeLabel;
}
