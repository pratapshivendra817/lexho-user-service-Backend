package com.lexho.user_service.cleaningcoverage.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record AdminCleaningCoverageRequest(
        @NotNull(message = "durationHours is required")
        @DecimalMin(value = "0.1", message = "durationHours must be greater than 0")
        BigDecimal durationHours,

        @NotBlank(message = "reference is required")
        @Size(max = 50, message = "reference must be at most 50 characters")
        String reference,

        @Min(value = 1, message = "sortOrder must be at least 1")
        Integer sortOrder,

        Boolean active,

        @Valid
        @NotEmpty(message = "tasks cannot be empty")
        List<AdminCleaningCoverageTaskRequest> tasks
) {
}
