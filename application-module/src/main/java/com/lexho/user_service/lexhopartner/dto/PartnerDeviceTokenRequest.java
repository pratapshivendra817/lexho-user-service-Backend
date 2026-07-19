package com.lexho.user_service.lexhopartner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PartnerDeviceTokenRequest(
        @NotBlank @Size(max = 512) String token,
        @NotBlank @Size(max = 30) String deviceType
) {
}
