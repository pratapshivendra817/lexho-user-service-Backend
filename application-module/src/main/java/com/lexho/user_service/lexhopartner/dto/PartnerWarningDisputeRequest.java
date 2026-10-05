package com.lexho.user_service.lexhopartner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PartnerWarningDisputeRequest(
        @NotBlank @Size(max = 1000) String reason,
        List<String> attachmentUrls
) {
}
