package com.lexho.user_service.homebanner.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HomeBannerReorderItemRequest {

    @NotNull(message = "id is required")
    private Long id;

    @NotNull(message = "priority is required")
    @Min(value = 0, message = "priority must be non-negative")
    private Integer priority;
}
