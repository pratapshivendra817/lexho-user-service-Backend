package com.lexho.user_service.subscription.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.subscription.dto.AdminPartnerAssignRequest;
import com.lexho.user_service.subscription.dto.AdminSubscriptionAssignmentResponse;
import com.lexho.user_service.subscription.dto.AdminSubscriptionDetailResponse;
import com.lexho.user_service.subscription.dto.AdminSubscriptionListResponse;
import com.lexho.user_service.subscription.dto.AdminSubscriptionStatusUpdateRequest;
import com.lexho.user_service.subscription.dto.AdminSubscriptionVisitStatusUpdateRequest;
import com.lexho.user_service.subscription.dto.SubscriptionCalendarItemResponse;
import com.lexho.user_service.subscription.enums.SubscriptionStatus;
import com.lexho.user_service.subscription.service.AdminSubscriptionManagementService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/admin/subscriptions", "/api/v1/admin/subscriptions"})
@PreAuthorize("hasRole('ADMIN')")
public class AdminSubscriptionController {

    private final AdminSubscriptionManagementService adminSubscriptionManagementService;

    public AdminSubscriptionController(AdminSubscriptionManagementService adminSubscriptionManagementService) {
        this.adminSubscriptionManagementService = adminSubscriptionManagementService;
    }

    @GetMapping
    public ApiResponse<Page<AdminSubscriptionListResponse>> list(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) SubscriptionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success("Subscriptions fetched",
                adminSubscriptionManagementService.list(customerId, status, page, size));
    }

    @GetMapping("/{subscriptionId}")
    public ApiResponse<AdminSubscriptionDetailResponse> detail(@PathVariable Long subscriptionId) {
        return ApiResponse.success("Subscription fetched",
                adminSubscriptionManagementService.detail(subscriptionId));
    }

    @GetMapping("/{subscriptionId}/calendar")
    public ApiResponse<List<SubscriptionCalendarItemResponse>> calendar(@PathVariable Long subscriptionId) {
        return ApiResponse.success("Subscription calendar fetched",
                adminSubscriptionManagementService.calendar(subscriptionId));
    }

    @PatchMapping("/{subscriptionId}/status")
    public ApiResponse<AdminSubscriptionDetailResponse> updateStatus(
            @PathVariable Long subscriptionId,
            @Valid @RequestBody AdminSubscriptionStatusUpdateRequest request) {
        return ApiResponse.success("Subscription status updated",
                adminSubscriptionManagementService.updateStatus(subscriptionId, request));
    }

    @PatchMapping("/{subscriptionId}/visits/{visitId}/status")
    public ApiResponse<SubscriptionCalendarItemResponse> updateVisitStatus(
            @PathVariable Long subscriptionId,
            @PathVariable Long visitId,
            @Valid @RequestBody AdminSubscriptionVisitStatusUpdateRequest request) {
        return ApiResponse.success("Visit status updated",
                adminSubscriptionManagementService.updateVisitStatus(subscriptionId, visitId, request));
    }

    @GetMapping("/{subscriptionId}/assignment")
    public ApiResponse<AdminSubscriptionAssignmentResponse> assignment(@PathVariable Long subscriptionId) {
        return ApiResponse.success("Subscription assignment fetched",
                adminSubscriptionManagementService.assignment(subscriptionId));
    }

    @PostMapping("/{subscriptionId}/assign-partner")
    public ApiResponse<AdminSubscriptionAssignmentResponse> assignPartner(
            @PathVariable Long subscriptionId,
            @Valid @RequestBody AdminPartnerAssignRequest request) {
        return ApiResponse.success("Partner assigned",
                adminSubscriptionManagementService.assignPartner(subscriptionId, request));
    }

    @PostMapping("/{subscriptionId}/replacement-partner")
    public ApiResponse<AdminSubscriptionAssignmentResponse> replacePartner(
            @PathVariable Long subscriptionId,
            @Valid @RequestBody AdminPartnerAssignRequest request) {
        return ApiResponse.success("Replacement partner assigned",
                adminSubscriptionManagementService.replacePartner(subscriptionId, request));
    }

    @PatchMapping("/{subscriptionId}/visits/{visitId}/assign-partner")
    public ApiResponse<?> assignVisitPartner(@PathVariable Long subscriptionId,
                                             @PathVariable Long visitId,
                                             @Valid @RequestBody AdminPartnerAssignRequest request) {
        return ApiResponse.success("Visit partner assigned",
                adminSubscriptionManagementService.assignVisitPartner(subscriptionId, visitId, request));
    }
}
