package com.lexho.user_service.addon.controller;

import com.lexho.user_service.addon.dto.AddonCategoryMappingRequest;
import com.lexho.user_service.addon.dto.AddonMappingResponse;
import com.lexho.user_service.addon.dto.AddonResponse;
import com.lexho.user_service.addon.dto.AddonServiceMappingRequest;
import com.lexho.user_service.addon.dto.AddonStatusRequest;
import com.lexho.user_service.addon.dto.AddonUpsertRequest;
import com.lexho.user_service.addon.service.AddonManagementService;
import com.lexho.user_service.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/addons")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Add-on Management")
public class AdminAddonController {
    private final AddonManagementService addonManagementService;

    public AdminAddonController(AddonManagementService addonManagementService) {
        this.addonManagementService = addonManagementService;
    }

    @PostMapping
    @Operation(summary = "Create add-on")
    public ApiResponse<AddonResponse> create(@Valid @RequestBody AddonUpsertRequest request) {
        return ApiResponse.success("Add-on created", addonManagementService.create(request));
    }

    @PutMapping("/{addonId}")
    @Operation(summary = "Update add-on")
    public ApiResponse<AddonResponse> update(@PathVariable Long addonId, @Valid @RequestBody AddonUpsertRequest request) {
        return ApiResponse.success("Add-on updated", addonManagementService.update(addonId, request));
    }

    @GetMapping("/{addonId}")
    @Operation(summary = "Get add-on details")
    public ApiResponse<AddonResponse> details(@PathVariable Long addonId) {
        return ApiResponse.success("Add-on fetched", addonManagementService.details(addonId));
    }

    @DeleteMapping("/{addonId}")
    @Operation(summary = "Delete add-on")
    public ApiResponse<Void> delete(@PathVariable Long addonId) {
        addonManagementService.delete(addonId);
        return ApiResponse.success("Add-on deleted", null);
    }

    @GetMapping
    @Operation(summary = "List add-ons")
    public ApiResponse<Map<String, Object>> list(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size,
                                                  @RequestParam(required = false) String search) {
        Page<AddonResponse> result = addonManagementService.list(page, size, search);
        return ApiResponse.success("Add-ons fetched", Map.of(
                "data", result.getContent(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages(),
                "pageNumber", result.getNumber()
        ));
    }

    @PatchMapping("/{addonId}/status")
    @Operation(summary = "Activate or deactivate add-on")
    public ApiResponse<AddonResponse> status(@PathVariable Long addonId, @Valid @RequestBody AddonStatusRequest request) {
        return ApiResponse.success("Add-on status updated", addonManagementService.updateStatus(addonId, request));
    }

    @PostMapping("/map/service")
    @Operation(summary = "Map add-on to one or more services")
    public ApiResponse<List<AddonMappingResponse>> mapService(@Valid @RequestBody AddonServiceMappingRequest request) {
        return ApiResponse.success("Add-on mapped to services", addonManagementService.mapToServices(request));
    }

    @PostMapping("/map/category")
    @Operation(summary = "Map add-on to one or more categories")
    public ApiResponse<List<AddonMappingResponse>> mapCategory(@Valid @RequestBody AddonCategoryMappingRequest request) {
        return ApiResponse.success("Add-on mapped to categories", addonManagementService.mapToCategories(request));
    }

    @DeleteMapping("/map/{mappingId}")
    @Operation(summary = "Remove add-on mapping")
    public ApiResponse<Void> removeMapping(@PathVariable Long mappingId) {
        addonManagementService.removeMapping(mappingId);
        return ApiResponse.success("Add-on mapping removed", null);
    }
}
