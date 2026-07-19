package com.lexho.user_service.lexhopartner.dto;

public record PartnerBankDetailsDto(
        String maskedAccountNumber,
        String ifscCode,
        boolean configured,
        String verificationStatus,
        String rejectionReason
) {
}
