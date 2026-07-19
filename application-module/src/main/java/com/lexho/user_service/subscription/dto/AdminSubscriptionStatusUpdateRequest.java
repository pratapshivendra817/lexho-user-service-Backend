package com.lexho.user_service.subscription.dto;

import com.lexho.user_service.subscription.enums.SubscriptionStatus;
import jakarta.validation.constraints.NotNull;

public record AdminSubscriptionStatusUpdateRequest(
        @NotNull(message = "status is required")
        SubscriptionStatus status
) {
}
