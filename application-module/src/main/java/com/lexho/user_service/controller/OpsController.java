package com.lexho.user_service.controller;

import com.lexho.user_service.dto.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ops")
public class OpsController {

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','PARTNER')")
    public ApiResponse<Map<String, String>> opsSummary() {
        return ApiResponse.success("Admin/Partner access granted", Map.of("scope", "ADMIN_OR_PARTNER"));
    }
}
