package com.lexho.user_service.lexhopartner.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdatePartnerProfileRequest(
        @JsonAlias("name")
        @NotBlank(message = "Full name is required")
        @Size(max = 120, message = "Full name is too long")
        String fullName,
        @JsonAlias({"phoneNumber", "mobileNumber"})
        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone must be 10-15 digits")
        String phone,
        @Email(message = "Invalid email")
        @Size(max = 120, message = "Email is too long")
        String email,
        @Size(max = 100, message = "City is too long")
        String city,
        @Pattern(regexp = "^(\\d{4,10})?$", message = "Pincode must be 4-10 digits")
        String pincode,
        @Size(max = 255, message = "Address is too long")
        String address,
        @Size(max = 80, message = "Service category is too long")
        String serviceCategory
) {
}
