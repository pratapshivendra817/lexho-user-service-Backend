package com.lexho.user_service.customer.profile.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

    @NotBlank(message = "fullName is required")
    @Size(max = 120, message = "fullName must be <= 120 characters")
    @Pattern(regexp = "^[A-Za-z\\s'-]+$", message = "fullName contains invalid characters")
    public String fullName;

    @Email(message = "Invalid email format")
    @Size(max = 150, message = "email must be <= 150 characters")
    public String email;

    @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "Invalid gender")
    public String gender;
}
