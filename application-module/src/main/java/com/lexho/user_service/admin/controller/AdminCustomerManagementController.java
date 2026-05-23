package com.lexho.user_service.admin.controller;

import com.lexho.user_service.admin.service.AdminCustomerManagementService;
import com.lexho.user_service.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/api/admin/customers", "/api/v1/admin/customers"})
@PreAuthorize("hasRole('ADMIN')")
public class AdminCustomerManagementController {

    private final AdminCustomerManagementService adminCustomerManagementService;

    public AdminCustomerManagementController(AdminCustomerManagementService adminCustomerManagementService) {
        this.adminCustomerManagementService = adminCustomerManagementService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(required = false) String search,
                                                  @RequestParam(required = false) Boolean active,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size,
                                                  @RequestParam(defaultValue = "updatedAt") String sortBy,
                                                  @RequestParam(defaultValue = "desc") String sortDir) {
        return ApiResponse.success("Customers fetched",
                adminCustomerManagementService.list(search, active, page, size, sortBy, sortDir));
    }

    @GetMapping("/{customerId}")
    public ApiResponse<Map<String, Object>> byId(@PathVariable Long customerId) {
        return ApiResponse.success("Customer fetched", adminCustomerManagementService.byId(customerId));
    }

    @PatchMapping("/{customerId}/status")
    public ApiResponse<Map<String, Object>> updateStatus(@PathVariable Long customerId,
                                                         @Valid @RequestBody CustomerStatusUpdateRequest request) {
        return ApiResponse.success("Customer status updated",
                adminCustomerManagementService.updateStatus(customerId, request.active));
    }

    @PatchMapping("/{customerId}/profile-governance")
    public ApiResponse<Map<String, Object>> profileGovernance(@PathVariable Long customerId,
                                                               @Valid @RequestBody ProfileGovernanceRequest request) {
        return ApiResponse.success("Customer profile governance updated",
                adminCustomerManagementService.profileGovernance(customerId, request.profileEditLocked,
                        request.emailVerified, request.phoneVerified, request.profileImageApproved, request.adminNotes));
    }

    @GetMapping("/account-delete-requests")
    public ApiResponse<Map<String, Object>> accountDeleteRequests(@RequestParam(required = false) Boolean resolved,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success("Account delete requests fetched",
                adminCustomerManagementService.listDeleteRequests(resolved, page, size));
    }

    @PatchMapping("/account-delete-requests/{requestId}/resolve")
    public ApiResponse<Map<String, Object>> resolveAccountDeleteRequest(@PathVariable Long requestId,
                                                                         @RequestAttribute(value = "userId", required = false) Long adminId,
                                                                         @Valid @RequestBody AccountDeleteResolveRequest request) {
        return ApiResponse.success("Account delete request resolved",
                adminCustomerManagementService.resolveDeleteRequest(requestId, request.approve, request.reason, adminId));
    }

    public static class CustomerStatusUpdateRequest {
        @NotNull(message = "active is required")
        public Boolean active;
    }

    public static class ProfileGovernanceRequest {
        public Boolean profileEditLocked;
        public Boolean emailVerified;
        public Boolean phoneVerified;
        public Boolean profileImageApproved;
        public String adminNotes;
    }

    public static class AccountDeleteResolveRequest {
        @NotNull(message = "approve is required")
        public Boolean approve;
        public String reason;
    }
}
