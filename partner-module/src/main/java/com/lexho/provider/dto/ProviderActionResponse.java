package com.lexho.partner.dto;

import com.lexho.partner.enums.OnboardingStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProviderActionResponse {
    private Long id;
    private String providerCode;
    private OnboardingStatus onboardingStatus;
    private String message;
}

