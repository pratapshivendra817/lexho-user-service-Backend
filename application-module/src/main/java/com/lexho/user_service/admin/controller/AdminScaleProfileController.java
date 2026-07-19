package com.lexho.user_service.admin.controller;

import com.lexho.user_service.admin.service.ScaleProfileService;
import com.lexho.user_service.dto.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/scale/profile")
@PreAuthorize("hasRole('ADMIN')")
public class AdminScaleProfileController {
    private final ScaleProfileService scaleProfileService;

    public AdminScaleProfileController(ScaleProfileService scaleProfileService) {
        this.scaleProfileService = scaleProfileService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> current() {
        return ApiResponse.success("Scale profile fetched", scaleProfileService.current());
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> apply(@RequestParam String profile,
                                                   @RequestParam(defaultValue = "true") boolean dryRun) {
        return ApiResponse.success("Scale profile evaluated", scaleProfileService.applyProfile(profile, dryRun));
    }
}

