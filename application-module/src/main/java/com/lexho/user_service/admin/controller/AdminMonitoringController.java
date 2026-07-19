package com.lexho.user_service.admin.controller;

import com.lexho.user_service.admin.dto.DataResponse;
import com.lexho.user_service.admin.service.AdminMonitoringService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping({"/api/admin/monitoring", "/api/v1/admin/monitoring"})
@PreAuthorize("hasRole('ADMIN')")
public class AdminMonitoringController {

    private final AdminMonitoringService adminMonitoringService;

    public AdminMonitoringController(AdminMonitoringService adminMonitoringService) {
        this.adminMonitoringService = adminMonitoringService;
    }

    @GetMapping("/dashboard")
    public DataResponse<Map<String, Object>> dashboard() {
        return new DataResponse<>(adminMonitoringService.dashboard());
    }
}
