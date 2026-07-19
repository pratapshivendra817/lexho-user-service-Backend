package com.lexho.user_service.subscription.controller;

import com.lexho.user_service.admin.dto.AdminMessageResponse;
import com.lexho.user_service.admin.dto.DataResponse;
import com.lexho.user_service.subscription.dto.CookCoverageRequest;
import com.lexho.user_service.subscription.dto.CookCoverageResponse;
import com.lexho.user_service.subscription.enums.FamilySize;
import com.lexho.user_service.subscription.service.CookCoverageService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CookCoverageController {

    private final CookCoverageService cookCoverageService;

    public CookCoverageController(CookCoverageService cookCoverageService) {
        this.cookCoverageService = cookCoverageService;
    }

    @GetMapping("/api/cook-coverage")
    public List<CookCoverageResponse> customerList(@RequestParam(required = false) Long serviceId,
                                                   @RequestParam(required = false) FamilySize familySize) {
        return cookCoverageService.customerList(serviceId, familySize);
    }

    @GetMapping("/api/admin/cook-coverage")
    @PreAuthorize("hasRole('ADMIN')")
    public DataResponse<List<CookCoverageResponse>> adminList() {
        return cookCoverageService.adminList();
    }

    @PostMapping("/api/admin/cook-coverage")
    @PreAuthorize("hasRole('ADMIN')")
    public DataResponse<CookCoverageResponse> create(@Valid @RequestBody CookCoverageRequest request) {
        return cookCoverageService.create(request);
    }

    @PutMapping("/api/admin/cook-coverage/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public DataResponse<CookCoverageResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody CookCoverageRequest request) {
        return cookCoverageService.update(id, request);
    }

    @DeleteMapping("/api/admin/cook-coverage/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminMessageResponse delete(@PathVariable Long id) {
        return cookCoverageService.delete(id);
    }
}
