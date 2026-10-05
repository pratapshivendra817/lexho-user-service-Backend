package com.lexho.user_service.support.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublicSupportTicketRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 190) String email,
        @Size(max = 30) String phone,
        @NotBlank @Size(max = 200) String subject,
        @NotBlank @Size(max = 2000) String message,
        @Size(max = 100) String bookingId
) {
}
