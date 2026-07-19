package com.lexho.provider.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
public class ProviderOnboardingRequest {

    @NotBlank(message = "Full name is required")
    @JsonAlias({"name"})
    @Size(max = 120, message = "Full name must be at most 120 characters")
    private String fullName;

    @Email(message = "Email must be valid")
    @NotBlank(message = "Email is required")
    @Size(max = 150, message = "Email must be at most 150 characters")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone number must be 10-15 digits")
    private String phoneNumber;

    @Size(max = 20, message = "Gender must be at most 20 characters")
    private String gender;

    private LocalDate dateOfBirth;

    @Size(max = 100, message = "Current city must be at most 100 characters")
    private String currentCity;

    @Size(max = 100, message = "Current state must be at most 100 characters")
    private String currentState;

    @Size(max = 500, message = "Current full address must be at most 500 characters")
    @JsonAlias({"current Address", "currentAddress"})
    private String currentFullAddress;

    @Size(max = 100, message = "City must be at most 100 characters")
    private String city;

    @Size(max = 100, message = "State must be at most 100 characters")
    private String state;

    @Size(max = 500, message = "Full address must be at most 500 characters")
    private String fullAddress;

    private Long serviceCategoryId;

    @Min(value = 0, message = "Experience years cannot be negative")
    @Max(value = 60, message = "Experience years cannot exceed 60")
    private Integer experienceYears;

    private List<@NotBlank(message = "Service area cannot be blank") String> serviceAreas;

    @JsonAlias({"serviceCategory", "serviceType "})
    private List<@NotBlank(message = "Service type cannot be blank") String> serviceType;

    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "^[0-9]{8,24}$", message = "Account number must be 8-24 digits")
    private String accountNumber;

    @NotBlank(message = "IFSC code is required")
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC code")
    private String ifscCode;

    public List<String> getServiceAreas() {
        return serviceAreas == null ? Collections.emptyList() : new ArrayList<>(serviceAreas);
    }

    public void setServiceAreas(List<String> serviceAreas) {
        this.serviceAreas = serviceAreas == null ? new ArrayList<>() : new ArrayList<>(serviceAreas);
    }

    public List<String> getServiceType() {
        return serviceType == null ? Collections.emptyList() : new ArrayList<>(serviceType);
    }

    public void setServiceType(List<String> serviceType) {
        this.serviceType = serviceType == null ? new ArrayList<>() : new ArrayList<>(serviceType);
    }
}
