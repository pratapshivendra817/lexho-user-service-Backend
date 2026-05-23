package com.lexho.partner.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProviderVerificationUpdateRequest {

    @NotBlank(message = "status is required")
    private String status;

    private String reason;
}
