package com.lexho.user_service.cleaningcoverage.dto;

public record AdminCleaningCoverageTaskResponse(
        Long id,
        String name,
        int estimatedMinutes,
        String icon,
        Integer sortOrder
) {
}
