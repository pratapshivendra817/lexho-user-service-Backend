package com.lexho.user_service.lexhopartner.dto;

public record PartnerAvailabilityResponse(
        boolean online,
        String updatedAt
) {
}
