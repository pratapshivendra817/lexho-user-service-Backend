package com.lexho.user_service.lexhopartner.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PartnerProfileDto(
        Long id,
        String name,
        String mobileNumber,
        String email,
        String city,
        String address,
        String serviceCategory,
        Double rating,
        String profileImage,
        boolean isOnline,
        boolean isVerified,
        LocalDateTime createdAt
) {
}
