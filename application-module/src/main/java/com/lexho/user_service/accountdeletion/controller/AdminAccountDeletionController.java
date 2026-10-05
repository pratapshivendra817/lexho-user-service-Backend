package com.lexho.user_service.accountdeletion.controller;

import com.lexho.user_service.accountdeletion.dto.AccountDeletionRejectRequest;
import com.lexho.user_service.accountdeletion.dto.AccountDeletionResponse;
import com.lexho.user_service.accountdeletion.service.AccountDeletionService;
import com.lexho.user_service.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/account-deletion")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAccountDeletionController {
    private final AccountDeletionService service;

    public AdminAccountDeletionController(AccountDeletionService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<AccountDeletionResponse>> list(@RequestParam(required = false) String status) {
        return ApiResponse.success("Account deletion requests fetched", service.adminList(status));
    }

    @GetMapping("/{id}")
    public ApiResponse<AccountDeletionResponse> get(@PathVariable Long id) {
        return ApiResponse.success("Account deletion request fetched", service.adminGet(id));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<AccountDeletionResponse> approve(@PathVariable Long id,
                                                        @RequestAttribute(value = "userId", required = false) Long adminId) {
        return ApiResponse.success("Account deletion request approved", service.approve(id, adminId));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<AccountDeletionResponse> reject(@PathVariable Long id,
                                                       @RequestAttribute(value = "userId", required = false) Long adminId,
                                                       @Valid @RequestBody AccountDeletionRejectRequest request) {
        return ApiResponse.success("Account deletion request rejected", service.reject(id, adminId, request));
    }

    @PostMapping("/{id}/process")
    public ApiResponse<AccountDeletionResponse> process(@PathVariable Long id,
                                                        @RequestAttribute(value = "userId", required = false) Long adminId) {
        return ApiResponse.success("Account deletion request processed", service.process(id, adminId));
    }
}
