package com.lexho.user_service.lexhopartner.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PartnerSupportReplyRequest(
        @NotNull @Size(max = 2000) String message,
        List<String> attachmentUrls
) {
}
