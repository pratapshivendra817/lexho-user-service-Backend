package com.lexho.user_service.subscription.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CookCoverageItemRequest(
        @NotBlank(message = "item name is required")
        @Size(max = 120, message = "item name must be at most 120 characters")
        String name,

        @NotBlank(message = "icon is required")
        @Size(max = 50, message = "icon must be at most 50 characters")
        String icon,

        @Min(value = 1, message = "sortOrder must be at least 1")
        Integer sortOrder,

        @Min(value = 1, message = "quantity must be at least 1")
        Integer quantity
) {
}
