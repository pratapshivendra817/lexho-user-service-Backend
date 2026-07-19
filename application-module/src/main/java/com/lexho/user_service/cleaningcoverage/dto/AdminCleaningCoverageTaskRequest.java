package com.lexho.user_service.cleaningcoverage.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminCleaningCoverageTaskRequest(
        @NotBlank(message = "task name is required")
        @Size(max = 150, message = "task name must be at most 150 characters")
        String name,

        @NotNull(message = "estimatedMinutes is required")
        @Min(value = 1, message = "estimatedMinutes must be at least 1")
        @Max(value = 1440, message = "estimatedMinutes must be at most 1440")
        Integer estimatedMinutes,

        @NotBlank(message = "icon is required")
        @Size(max = 50, message = "icon must be at most 50 characters")
        String icon,

        @Min(value = 1, message = "sortOrder must be at least 1")
        Integer sortOrder
) {
}
