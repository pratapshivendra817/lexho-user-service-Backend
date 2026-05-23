package com.lexho.user_service.lexhopartner.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePartnerProfileRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 120, message = "Name is too long")
        String name,
        @Email(message = "Invalid email")
        @Size(max = 120, message = "Email is too long")
        String email,
        @Size(max = 100, message = "City is too long")
        String city,
        @Size(max = 255, message = "Address is too long")
        String address,
        @Size(max = 80, message = "Service category is too long")
        String serviceCategory
) {
}
