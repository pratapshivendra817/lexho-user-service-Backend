package com.lexho.user_service.cleaningcoverage.controller;

import com.lexho.user_service.admin.dto.AdminMessageResponse;
import com.lexho.user_service.admin.dto.DataResponse;
import com.lexho.user_service.cleaningcoverage.dto.AdminCleaningCoverageRequest;
import com.lexho.user_service.cleaningcoverage.dto.AdminCleaningCoverageResponse;
import com.lexho.user_service.cleaningcoverage.service.CleaningCoverageService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/admin/cleaning-coverage", "/api/v1/admin/cleaning-coverage"})
@PreAuthorize("hasRole('ADMIN')")
public class AdminCleaningCoverageController {

    private final CleaningCoverageService cleaningCoverageService;

    public AdminCleaningCoverageController(CleaningCoverageService cleaningCoverageService) {
        this.cleaningCoverageService = cleaningCoverageService;
    }

    @GetMapping
    public DataResponse<List<AdminCleaningCoverageResponse>> list() {
        return cleaningCoverageService.adminList();
    }

    @PostMapping
    public DataResponse<AdminCleaningCoverageResponse> create(@Valid @RequestBody AdminCleaningCoverageRequest request) {
        return cleaningCoverageService.create(request);
    }

    @PutMapping("/{id}")
    public DataResponse<AdminCleaningCoverageResponse> update(@PathVariable Long id,
                                                              @Valid @RequestBody AdminCleaningCoverageRequest request) {
        return cleaningCoverageService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public AdminMessageResponse delete(@PathVariable Long id) {
        return cleaningCoverageService.delete(id);
    }
}
