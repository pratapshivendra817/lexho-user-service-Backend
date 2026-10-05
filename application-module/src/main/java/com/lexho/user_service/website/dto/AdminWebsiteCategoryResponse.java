package com.lexho.user_service.website.dto;

public record AdminWebsiteCategoryResponse(
        Long id,
        String name,
        String slug,
        boolean active,
        boolean showOnWebsite,
        int displayOrder
) {
}
