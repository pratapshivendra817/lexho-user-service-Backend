package com.lexho.user_service.customer.dto;

import com.lexho.user_service.addon.dto.AddonSelectionRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
public class CustomerAddToCartRequest {
    @NotNull(message = "serviceId is required")
    private Long serviceId;

    private Long familySizeId;
    private String familySize;
    private String familySizeLabel;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;

    @Valid
    private List<AddonSelectionRequest> addons;
}
