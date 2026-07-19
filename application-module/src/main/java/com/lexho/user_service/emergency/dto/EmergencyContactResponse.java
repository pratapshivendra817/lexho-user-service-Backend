package com.lexho.user_service.emergency.dto;

public record EmergencyContactResponse(
        Long id,
        Long partnerId,
        String name,
        String phoneNumber,
        String relationship,
        String email,
        boolean primaryContact,
        boolean active
) {
}
