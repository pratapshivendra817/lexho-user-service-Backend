package com.lexho.partner.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProviderBankDetailsResponse {
    private Long providerId;
    private String providerCode;
    private String accountNumber;
    private String ifscCode;
}
