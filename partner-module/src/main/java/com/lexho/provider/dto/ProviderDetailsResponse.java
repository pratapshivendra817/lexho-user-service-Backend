package com.lexho.provider.dto;

import com.lexho.provider.enums.KycStatus;
import com.lexho.provider.enums.OnboardingStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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

    public List<String> getServiceAreas() {
        return serviceAreas == null ? Collections.emptyList() : new ArrayList<>(serviceAreas);
    }

    public void setServiceAreas(List<String> serviceAreas) {
        this.serviceAreas = serviceAreas == null ? new ArrayList<>() : new ArrayList<>(serviceAreas);
    }

    public List<ProviderDocumentResponse> getDocuments() {
        return documents == null ? Collections.emptyList() : new ArrayList<>(documents);
    }

    public void setDocuments(List<ProviderDocumentResponse> documents) {
        this.documents = documents == null ? new ArrayList<>() : new ArrayList<>(documents);
    }
}
