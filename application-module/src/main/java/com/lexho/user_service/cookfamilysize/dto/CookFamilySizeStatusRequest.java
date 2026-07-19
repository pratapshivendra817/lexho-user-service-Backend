package com.lexho.user_service.cookfamilysize.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CookFamilySizeStatusRequest {
    @NotNull(message = "active is required")
    private Boolean active;
}
