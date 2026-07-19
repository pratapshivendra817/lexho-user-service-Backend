package com.lexho.user_service.admin.controller;

import com.lexho.user_service.cartconfig.dto.CartConfigRequest;
import com.lexho.user_service.cartconfig.dto.CartConfigResponse;
import com.lexho.user_service.cartconfig.service.CartConfigService;
import com.lexho.user_service.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/cart-config")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCartConfigController {
    private final CartConfigService cartConfigService;

    public AdminCartConfigController(CartConfigService cartConfigService) {
        this.cartConfigService = cartConfigService;
    }

    @GetMapping
    public ApiResponse<CartConfigResponse> get() {
        return ApiResponse.success("Cart config fetched", cartConfigService.current());
    }

    @PutMapping
    public ApiResponse<CartConfigResponse> update(@Valid @RequestBody CartConfigRequest request) {
        return ApiResponse.success("Cart config fetched", cartConfigService.update(request));
    }
}
