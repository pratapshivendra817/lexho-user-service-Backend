package com.lexho.user_service.section.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SectionStatusRequest {
    @NotNull(message = "isActive is required")
    private Boolean isActive;
}
