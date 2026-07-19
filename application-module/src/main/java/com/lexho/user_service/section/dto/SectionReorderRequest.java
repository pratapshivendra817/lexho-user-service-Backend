package com.lexho.user_service.section.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SectionReorderRequest {
    @NotNull(message = "id is required")
    private Long id;

    @NotNull(message = "priority is required")
    private Integer priority;
}
