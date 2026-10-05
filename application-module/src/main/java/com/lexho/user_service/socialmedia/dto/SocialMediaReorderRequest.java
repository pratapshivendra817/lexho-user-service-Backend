package com.lexho.user_service.socialmedia.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SocialMediaReorderRequest {
    @NotNull(message = "id is required")
    private Long id;

    @NotNull(message = "displayOrder is required")
    @Min(value = 1, message = "displayOrder must be greater than 0")
    private Integer displayOrder;
}
