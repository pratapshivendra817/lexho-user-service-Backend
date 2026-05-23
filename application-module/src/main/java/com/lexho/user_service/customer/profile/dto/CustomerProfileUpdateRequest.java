package com.lexho.user_service.customer.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CustomerProfileUpdateRequest {
    @NotBlank(message = "First name is required")
    @Pattern(regexp = "^[A-Za-z\\s'-]{1,100}$", message = "First name contains invalid characters")
    public String firstName;
    @Pattern(regexp = "^[A-Za-z\\s'-]{0,100}$", message = "Last name contains invalid characters")
    public String lastName;
    public String gender;
    public String dateOfBirth;
}

