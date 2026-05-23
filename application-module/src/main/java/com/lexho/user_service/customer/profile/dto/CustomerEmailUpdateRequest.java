package com.lexho.user_service.customer.profile.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class CustomerEmailUpdateRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    public String email;
}

