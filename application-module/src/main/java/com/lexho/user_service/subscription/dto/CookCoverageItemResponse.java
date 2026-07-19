package com.lexho.user_service.subscription.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CookCoverageItemResponse(
        Long id,
        String name,
        String icon,
        Integer sortOrder,
        Integer quantity
) {
}
