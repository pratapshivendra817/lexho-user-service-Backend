package com.lexho.user_service.cleaningcoverage.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdminCleaningCoverageResponse(
        Long id,
        BigDecimal durationHours,
        String reference,
        Integer sortOrder,
        boolean active,
        List<AdminCleaningCoverageTaskResponse> tasks
) {
}
