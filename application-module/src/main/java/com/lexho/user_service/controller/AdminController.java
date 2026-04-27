package com.lexho.user_service.controller;

import com.lexho.user_service.dto.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, String>> adminDashboard() {
        return ApiResponse.success("Admin access granted", Map.of("scope", "ADMIN"));
    }
}
