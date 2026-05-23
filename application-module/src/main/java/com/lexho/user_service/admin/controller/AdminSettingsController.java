package com.lexho.user_service.admin.controller;

import com.lexho.user_service.admin.settings.service.AdminSettingsService;
import com.lexho.user_service.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/admin/settings", "/api/v1/admin/settings"})
@PreAuthorize("hasRole('ADMIN')")
public class AdminSettingsController {

    private final AdminSettingsService adminSettingsService;

    public AdminSettingsController(AdminSettingsService adminSettingsService) {
        this.adminSettingsService = adminSettingsService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> all() {
        return ApiResponse.success("Settings fetched", adminSettingsService.listAll());
    }

    @GetMapping("/{key}")
    public ApiResponse<Map<String, Object>> byKey(@PathVariable String key) {
        return ApiResponse.success("Setting fetched", adminSettingsService.get(key));
    }

    @PutMapping("/{key}")
    public ApiResponse<Map<String, Object>> upsert(@PathVariable String key,
                                                    @RequestAttribute(value = "userId", required = false) Long adminId,
                                                    @Valid @RequestBody SettingUpsertRequest request) {
        return ApiResponse.success("Setting saved",
                adminSettingsService.upsert(key, request.value, request.description, adminId == null ? "ADMIN" : "ADMIN:" + adminId));
    }

    @PutMapping("/dispatch-radius")
    public ApiResponse<Map<String, Object>> dispatchRadius(@RequestAttribute(value = "userId", required = false) Long adminId,
                                                            @Valid @RequestBody RadiusRequest request) {
        return ApiResponse.success("Dispatch radius saved",
                adminSettingsService.updateDispatchRadius(request.radiusKm, adminId == null ? "ADMIN" : "ADMIN:" + adminId));
    }

    @PutMapping("/partner-radius")
    public ApiResponse<Map<String, Object>> partnerRadius(@RequestAttribute(value = "userId", required = false) Long adminId,
                                                           @Valid @RequestBody RadiusRequest request) {
        return ApiResponse.success("Partner service radius saved",
                adminSettingsService.updatePartnerServiceRadius(request.radiusKm, adminId == null ? "ADMIN" : "ADMIN:" + adminId));
    }

    public static class SettingUpsertRequest {
        @NotBlank(message = "value is required")
        public String value;
        public String description;
    }

    public static class RadiusRequest {
        @NotNull(message = "radiusKm is required")
        public Double radiusKm;
    }
}

