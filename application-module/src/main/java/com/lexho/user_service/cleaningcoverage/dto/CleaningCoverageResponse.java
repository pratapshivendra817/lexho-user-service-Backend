package com.lexho.user_service.cleaningcoverage.dto;

import java.math.BigDecimal;
import java.util.List;

public record CleaningCoverageResponse(
        BigDecimal durationHours,
        String reference,
        List<CleaningCoverageTaskResponse> tasks
) {
}
