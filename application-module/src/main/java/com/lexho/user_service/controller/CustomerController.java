package com.lexho.user_service.controller;

import com.lexho.user_service.dto.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<Map<String, String>> customerDashboard() {
        return ApiResponse.success("Customer access granted", Map.of("scope", "CUSTOMER"));
    }
}
