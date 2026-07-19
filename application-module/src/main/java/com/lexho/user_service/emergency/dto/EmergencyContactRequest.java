package com.lexho.user_service.emergency.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmergencyContactRequest(
        @NotBlank
        @Size(max = 120)
        String name,
        @NotBlank
        @Pattern(regexp = "^[0-9+ -]{7,20}$", message = "phoneNumber is invalid")
        String phoneNumber,
        @Size(max = 80)
        String relationship,
        @Email
        @Size(max = 150)
        String email,
        boolean primaryContact
) {
}
