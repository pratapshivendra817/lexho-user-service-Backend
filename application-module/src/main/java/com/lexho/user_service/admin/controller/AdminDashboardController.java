package com.lexho.user_service.admin.controller;

import com.lexho.user_service.admin.dto.DataResponse;
import com.lexho.user_service.admin.dto.AdminServiceRatingAnalyticsResponse;
import com.lexho.user_service.admin.dto.AdminServiceResponse;
import com.lexho.user_service.admin.dto.AdminServiceSummaryResponse;
import com.lexho.user_service.admin.service.AdminDashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/admin/dashboard", "/api/v1/admin/dashboard"})
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/stats")
    public DataResponse<Map<String, Object>> stats() {
        return new DataResponse<>(adminDashboardService.dashboardStats());
    }

    @GetMapping("/revenue")
    public DataResponse<List<Map<String, Object>>> revenue() {
        return new DataResponse<>(adminDashboardService.monthlyRevenue());
    }

    @GetMapping("/services-distribution")
    public DataResponse<List<Map<String, Object>>> serviceDistribution() {
        return new DataResponse<>(adminDashboardService.serviceDistribution());
    }

    @GetMapping("/service-summary")
    public DataResponse<List<AdminServiceSummaryResponse>> serviceSummary() {
        return adminDashboardService.serviceSummary();
    }

    @GetMapping("/top-services")
    public DataResponse<List<AdminServiceResponse>> topServices(@RequestParam(defaultValue = "10") int limit) {
        return adminDashboardService.topServices(limit);
    }

    @GetMapping("/service-analytics")
    public DataResponse<List<AdminServiceRatingAnalyticsResponse>> serviceAnalytics() {
        return adminDashboardService.serviceAnalytics();
    }
}
