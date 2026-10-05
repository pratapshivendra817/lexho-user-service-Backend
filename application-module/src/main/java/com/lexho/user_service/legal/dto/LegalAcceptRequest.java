package com.lexho.user_service.legal.dto;

import jakarta.validation.constraints.NotBlank;

public record LegalAcceptRequest(
        @NotBlank(message = "documentType is required") String documentType,
        Integer documentVersion) {
}
