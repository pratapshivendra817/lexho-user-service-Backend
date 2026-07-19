package com.lexho.user_service.emergency.dto;

import com.lexho.user_service.emergency.entity.EmergencyEventStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record EmergencyEventResponse(
        Long eventId,
        Long id,
        String incidentCode,
        EmergencyIncidentPartnerView partner,
        Long partnerId,
        String partnerName,
        String partnerPhone,
        String partnerMobileNumber,
        EmergencyEventStatus status,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal accuracyMeters,
        String address,
        EmergencyIncidentLocationView location,
        String message,
        LocalDateTime createdAt,
        LocalDateTime triggeredAt,
        String acknowledgementNote,
        String acknowledgeNote,
        String resolutionNote,
        String resolveNote,
        LocalDateTime acknowledgedAt,
        String acknowledgedBy,
        LocalDateTime resolvedAt,
        String resolvedBy,
        String requestId,
        String correlationId,
        boolean adminAlertSent,
        int notifiedContactsCount,
        EmergencyLocationResponse latestLocation,
        List<EmergencyLocationResponse> locations
) {
}
