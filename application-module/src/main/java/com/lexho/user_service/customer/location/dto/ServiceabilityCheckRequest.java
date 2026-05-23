package com.lexho.user_service.customer.location.dto;

import jakarta.validation.constraints.NotBlank;

public class ServiceabilityCheckRequest {
    @NotBlank
    public String pincode;
}
