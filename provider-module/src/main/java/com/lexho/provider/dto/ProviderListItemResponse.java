package com.lexho.provider.dto;

import com.lexho.provider.enums.KycStatus;
import com.lexho.provider.enums.OnboardingStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ProviderListItemResponse {
    private Long id;
    private String providerCode;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String city;
    private String serviceType;
    private Integer experienceYears;
    private KycStatus kycStatus;
    private OnboardingStatus onboardingStatus;
    private LocalDateTime createdAt;
}

