package com.lexho.user_service.subscription.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CookCoverageResponse(
        Long id,
        Long serviceId,
        String serviceName,
        String familySize,
        String members,
        String familySizeLabel,
        String reference,
        Integer sortOrder,
        Boolean active,
        List<CookCoverageItemResponse> items
) {
}
