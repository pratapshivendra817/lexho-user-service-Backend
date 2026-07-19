package com.lexho.user_service.cleaningcoverage.controller;

import com.lexho.user_service.cleaningcoverage.dto.CleaningCoverageResponse;
import com.lexho.user_service.cleaningcoverage.service.CleaningCoverageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
public class CleaningCoverageController {

    private final CleaningCoverageService cleaningCoverageService;

    public CleaningCoverageController(CleaningCoverageService cleaningCoverageService) {
        this.cleaningCoverageService = cleaningCoverageService;
    }

    @GetMapping("/api/cleaning-coverage")
    public List<CleaningCoverageResponse> list(@RequestParam(required = false) BigDecimal durationHours) {
        return cleaningCoverageService.list(Optional.ofNullable(durationHours));
    }
}
