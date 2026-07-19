package com.lexho.user_service.subscription.dto;

import com.lexho.user_service.subscription.enums.FamilySize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CookCoverageRequest(
        Long serviceId,

        @NotNull(message = "familySize is required")
        FamilySize familySize,

        @NotBlank(message = "members is required")
        @Size(max = 30, message = "members must be at most 30 characters")
        String members,

        @NotBlank(message = "familySizeLabel is required")
        @Size(max = 50, message = "familySizeLabel must be at most 50 characters")
        String familySizeLabel,

        @Size(max = 150, message = "reference must be at most 150 characters")
        String reference,

        @Min(value = 1, message = "sortOrder must be at least 1")
        Integer sortOrder,

        Boolean active,

        @Valid
        @NotEmpty(message = "items cannot be empty")
        List<CookCoverageItemRequest> items
) {
}
