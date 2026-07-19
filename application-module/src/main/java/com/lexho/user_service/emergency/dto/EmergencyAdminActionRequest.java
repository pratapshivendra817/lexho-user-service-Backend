package com.lexho.user_service.emergency.dto;

import jakarta.validation.constraints.Size;

public record EmergencyAdminActionRequest(
        @Size(max = 500)
        String note
) {
}
