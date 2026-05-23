package com.lexho.user_service.lexhopartner.dto;

import com.lexho.user_service.lexhopartner.entity.DocumentType;
import com.lexho.user_service.lexhopartner.entity.VerificationStatus;
import lombok.Builder;

@Builder
public record PartnerDocumentDto(
        Long id,
        DocumentType type,
        String url,
        VerificationStatus verificationStatus
) {
}
