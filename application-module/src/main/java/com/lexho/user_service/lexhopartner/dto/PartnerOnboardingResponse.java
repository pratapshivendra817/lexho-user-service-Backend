package com.lexho.user_service.lexhopartner.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record PartnerOnboardingResponse(
        Long partnerId,
        String fullName,
        String mobileNumber,
        LocalDate dateOfBirth,
        String gender,
        String addressLine1,
        String city,
        String state,
        String pincode,
        Long serviceCategoryId,
        List<String> skills,
        String maskedAccountNumber,
        String accountNumber,
        String ifscCode,
        String bankVerificationStatus,
        String aadhaarFrontUrl,
        String aadhaarBackUrl,
        String panCardUrl,
        String selfieUrl,
        String certificateUrl,
        String applicationStatus,
        String documentStatus,
        String rejectionReason,
        Long reviewedByAdminId,
        LocalDateTime reviewedAt,
        LocalDateTime submittedAt,
        List<PartnerOnboardingDocumentResponse> documents,
        Map<String, PartnerOnboardingDocumentResponse> documentMap
) {
    public PartnerOnboardingResponse(
            Long partnerId,
            String fullName,
            String mobileNumber,
            LocalDate dateOfBirth,
            String gender,
            String addressLine1,
            String city,
            String state,
            String pincode,
            Long serviceCategoryId,
            List<String> skills,
            String maskedAccountNumber,
            String ifscCode,
            String bankVerificationStatus,
            String aadhaarFrontUrl,
            String aadhaarBackUrl,
            String panCardUrl,
            String selfieUrl,
            String certificateUrl,
            String applicationStatus,
            String documentStatus,
            String rejectionReason,
            Long reviewedByAdminId,
            LocalDateTime reviewedAt,
            LocalDateTime submittedAt) {
        this(partnerId, fullName, mobileNumber, dateOfBirth, gender, addressLine1, city, state, pincode,
                serviceCategoryId, skills, maskedAccountNumber, null, ifscCode, bankVerificationStatus,
                aadhaarFrontUrl, aadhaarBackUrl, panCardUrl, selfieUrl, certificateUrl, applicationStatus,
                documentStatus, rejectionReason, reviewedByAdminId, reviewedAt, submittedAt,
                List.of(), Map.of());
    }
}
