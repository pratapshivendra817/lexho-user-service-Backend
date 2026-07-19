package com.lexho.user_service.cleaningcoverage.dto;

public record CleaningCoverageTaskResponse(
        String name,
        int estimatedMinutes,
        String icon
) {
}
