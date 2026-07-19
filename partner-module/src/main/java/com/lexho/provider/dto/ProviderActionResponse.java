package com.lexho.provider.dto;

import com.lexho.provider.enums.OnboardingStatus;
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

