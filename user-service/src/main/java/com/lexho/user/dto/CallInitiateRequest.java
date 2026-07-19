package com.lexho.user.dto;

import jakarta.validation.constraints.NotNull;

public record CallInitiateRequest(@NotNull Long receiverId) {
}
