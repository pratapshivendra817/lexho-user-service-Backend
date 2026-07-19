package com.lexho.user_service.subscription.dto;

import com.lexho.user_service.subscription.enums.VisitStatus;
import jakarta.validation.constraints.NotNull;

public record AdminSubscriptionVisitStatusUpdateRequest(
        @NotNull(message = "status is required")
        VisitStatus status
) {
}
