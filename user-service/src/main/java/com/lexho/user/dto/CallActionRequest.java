package com.lexho.user.dto;

import jakarta.validation.constraints.NotNull;

public record CallActionRequest(@NotNull Long sessionId) {
}
