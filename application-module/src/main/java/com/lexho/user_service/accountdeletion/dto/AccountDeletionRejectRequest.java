package com.lexho.user_service.accountdeletion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccountDeletionRejectRequest(
        @NotBlank(message = "reason is required") @Size(max = 500) String reason) {
}
