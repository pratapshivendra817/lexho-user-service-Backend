package com.lexho.user_service.accountdeletion.dto;

import jakarta.validation.constraints.Size;

public record AccountDeletionCreateRequest(@Size(max = 500) String reason) {
}
