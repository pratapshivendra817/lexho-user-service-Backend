package com.lexho.partner.dto;

import com.lexho.partner.enums.KycStatus;
import com.lexho.partner.enums.OnboardingStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ProviderDetailsResponse {
    private Long id;
    private String providerCode;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String gender;
    private LocalDate dateOfBirth;
    private String currentCity;
    private String currentState;
    private String currentFullAddress;
    private String city;
    private String state;
    private String fullAddress;
    private Long serviceCategoryId;
    private String serviceCategoryName;
    private Integer experienceYears;
    private List<String> serviceAreas;
    private String accountNumberMasked;
    private String ifscCode;
    private KycStatus kycStatus;
    private String kycFailedReason;
    private OnboardingStatus onboardingStatus;
    private String rejectionReason;
    private String blockedReason;
    private boolean online;
    private List<ProviderDocumentResponse> documents;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
