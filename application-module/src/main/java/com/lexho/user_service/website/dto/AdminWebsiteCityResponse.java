package com.lexho.user_service.website.dto;

public record AdminWebsiteCityResponse(
        Long id,
        String name,
        String slug,
        boolean active,
        int displayOrder
) {
}
