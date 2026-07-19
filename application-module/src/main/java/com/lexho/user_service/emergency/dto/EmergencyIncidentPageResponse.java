package com.lexho.user_service.emergency.dto;

import java.util.List;

public record EmergencyIncidentPageResponse(
        List<EmergencyEventResponse> content,
        int page,
        int limit,
        long totalElements,
        int totalPages
) {
}
