package com.lexho.user_service.admin.controller;

import com.lexho.user_service.admin.service.RestrictedAreaOpsService;
import com.lexho.user_service.dto.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/serviceability")
@PreAuthorize("hasRole('ADMIN')")
public class AdminServiceabilityOpsController {
    private final RestrictedAreaOpsService restrictedAreaOpsService;

    public AdminServiceabilityOpsController(RestrictedAreaOpsService restrictedAreaOpsService) {
        this.restrictedAreaOpsService = restrictedAreaOpsService;
    }

    @PostMapping("/restricted-area")
    public ApiResponse<Map<String, Object>> addRestrictedArea(@RequestParam double lat,
                                                               @RequestParam double lng,
                                                               @RequestParam(defaultValue = "6") int precision) {
        return ApiResponse.success("Restricted area added", restrictedAreaOpsService.add(lat, lng, precision));
    }

    @DeleteMapping("/restricted-area")
    public ApiResponse<Map<String, Object>> removeRestrictedArea(@RequestParam String geohash) {
        return ApiResponse.success("Restricted area removed", restrictedAreaOpsService.remove(geohash));
    }
}
