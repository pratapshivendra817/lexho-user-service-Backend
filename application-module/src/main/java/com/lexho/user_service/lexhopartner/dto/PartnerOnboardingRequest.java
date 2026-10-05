package com.lexho.user_service.lexhopartner.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record PartnerOnboardingRequest(
        @NotBlank(message = "fullName is required")
        @Size(max = 120, message = "fullName must be at most 120 characters")
        String fullName,

        @Pattern(regexp = "^[0-9]{10}$", message = "phoneNumber must contain 10 digits")
        String phoneNumber,

        @NotNull(message = "dateOfBirth is required")
        @Past(message = "dateOfBirth must be in the past")
        LocalDate dateOfBirth,

        @NotBlank(message = "gender is required")
        @Size(max = 20, message = "gender must be at most 20 characters")
        String gender,

        @NotBlank(message = "addressLine1 is required")
        @Size(max = 500, message = "addressLine1 must be at most 500 characters")
        @JsonAlias({"address", "address_line1", "fullAddress"})
        String addressLine1,

        @NotBlank(message = "city is required")
        @Size(max = 100, message = "city must be at most 100 characters")
        String city,

        @NotBlank(message = "state is required")
        @Size(max = 100, message = "state must be at most 100 characters")
        String state,

        @Pattern(regexp = "^[0-9]{6}$", message = "pincode must contain 6 digits")
        String pincode,

        @JsonAlias({"service_category_id", "categoryId"})
        Long serviceCategoryId,

        @NotEmpty(message = "skills is required")
        @JsonAlias({"serviceType", "serviceTypes", "skill"})
        List<@NotBlank(message = "skills must not contain blank values")
                @Size(max = 100, message = "each skill must be at most 100 characters") String> skills,

        @NotBlank(message = "accountNumber is required")
        @Pattern(regexp = "^[0-9]{9,18}$", message = "accountNumber must contain 9 to 18 digits")
        @JsonAlias({"account_number", "bankAccountNumber"})
        String accountNumber,

        @NotBlank(message = "ifscCode is required")
        @Pattern(regexp = "^[A-Za-z]{4}0[A-Za-z0-9]{6}$", message = "ifscCode is invalid")
        @JsonAlias({"ifsc", "ifsc_code"})
        String ifscCode,

        @Size(max = 1000, message = "aadhaarFrontUrl must be at most 1000 characters")
        @JsonAlias({"aadhaarUrl", "aadhaarFront", "aadhaar_front_url", "aadhaar_front", "aadhaarImage"})
        String aadhaarFrontUrl,

        @Size(max = 1000, message = "aadhaarBackUrl must be at most 1000 characters")
        @JsonAlias({"aadhaarBack", "aadhaarBackImage", "aadhaar_back_url", "aadhaar_back", "aadharBack", "aadharBackImage"})
        String aadhaarBackUrl,

        @Size(max = 1000, message = "panCardUrl must be at most 1000 characters")
        @JsonAlias({"panUrl", "panCard", "panCardImage", "pan_card_url", "pan_card", "panImage"})
        String panCardUrl,

        @Size(max = 1000, message = "selfieUrl must be at most 1000 characters")
        @JsonAlias({"selfie", "selfieImage", "selfiePhoto", "selfie_url", "selfie_photo"})
        String selfieUrl,

        @Size(max = 1000, message = "certificateUrl must be at most 1000 characters")
        @JsonAlias({"certificate", "certificateImage", "certificateFile", "certificate_url"})
        String certificateUrl
) {
}
