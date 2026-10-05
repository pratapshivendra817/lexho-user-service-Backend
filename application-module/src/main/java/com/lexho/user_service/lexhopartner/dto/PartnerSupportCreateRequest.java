package com.lexho.user_service.lexhopartner.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PartnerSupportCreateRequest(
        @NotNull @Size(max = 200) String subject,
        @NotNull @Size(max = 2000) String message,
        @Size(max = 100) String categoryId,
        @Size(max = 100) String bookingId,
        @Pattern(regexp = "LOW|MEDIUM|HIGH|URGENT", message = "priority must be LOW, MEDIUM, HIGH or URGENT")
        String priority,
        List<String> attachmentUrls
) {
}
