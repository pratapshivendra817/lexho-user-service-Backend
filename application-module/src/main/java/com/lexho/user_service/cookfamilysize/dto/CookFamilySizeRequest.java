package com.lexho.user_service.cookfamilysize.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CookFamilySizeRequest {
    @NotBlank(message = "familySize is required")
    @Size(max = 30, message = "familySize must be at most 30 characters")
    private String familySize;

    @NotBlank(message = "familySizeLabel is required")
    @Size(max = 100, message = "familySizeLabel must be at most 100 characters")
    private String familySizeLabel;

    @NotNull(message = "active is required")
    private Boolean active;

    @NotNull(message = "displayOrder is required")
    @Min(value = 0, message = "displayOrder must be zero or positive")
    private Integer displayOrder;
}
