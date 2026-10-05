package com.lexho.user_service.accountdeletion.controller;

import com.lexho.user_service.accountdeletion.dto.AccountDeletionCreateRequest;
import com.lexho.user_service.accountdeletion.dto.AccountDeletionResponse;
import com.lexho.user_service.accountdeletion.service.AccountDeletionService;
import com.lexho.user_service.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/account/delete-request")
public class AccountDeletionController {
    private final AccountDeletionService service;

    public AccountDeletionController(AccountDeletionService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<AccountDeletionResponse> request(@RequestAttribute("userId") Long userId,
                                                        @RequestAttribute("role") String role,
                                                        @Valid @RequestBody(required = false) AccountDeletionCreateRequest request) {
        return ApiResponse.success("Account deletion request received", service.request(userId, role, request));
    }

    @GetMapping("/status")
    public ApiResponse<AccountDeletionResponse> status(@RequestAttribute("userId") Long userId,
                                                       @RequestAttribute("role") String role) {
        return ApiResponse.success("Account deletion status fetched", service.status(userId, role));
    }

    @PostMapping("/cancel")
    public ApiResponse<AccountDeletionResponse> cancel(@RequestAttribute("userId") Long userId,
                                                       @RequestAttribute("role") String role) {
        return ApiResponse.success("Account deletion request cancelled", service.cancel(userId, role));
    }
}
