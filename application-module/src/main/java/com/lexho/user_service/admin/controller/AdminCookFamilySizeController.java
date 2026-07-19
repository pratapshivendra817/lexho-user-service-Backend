package com.lexho.user_service.admin.controller;

import com.lexho.user_service.cookfamilysize.dto.CookFamilySizeAdminResponse;
import com.lexho.user_service.cookfamilysize.dto.CookFamilySizeRequest;
import com.lexho.user_service.cookfamilysize.dto.CookFamilySizeStatusRequest;
import com.lexho.user_service.cookfamilysize.service.CookFamilySizeService;
import com.lexho.user_service.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/cook-family-sizes")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCookFamilySizeController {
    private final CookFamilySizeService cookFamilySizeService;

    public AdminCookFamilySizeController(CookFamilySizeService cookFamilySizeService) {
        this.cookFamilySizeService = cookFamilySizeService;
    }

    @GetMapping
    public ApiResponse<List<CookFamilySizeAdminResponse>> list() {
        return ApiResponse.success("Cook family sizes fetched", cookFamilySizeService.adminList());
    }

    @PostMapping
    public ApiResponse<CookFamilySizeAdminResponse> create(@Valid @RequestBody CookFamilySizeRequest request) {
        return ApiResponse.success("Cook family size created", cookFamilySizeService.create(request));
    }

    @PutMapping("/{familySizeId}")
    public ApiResponse<CookFamilySizeAdminResponse> update(@PathVariable Long familySizeId,
                                                           @Valid @RequestBody CookFamilySizeRequest request) {
        return ApiResponse.success("Cook family size updated",
                cookFamilySizeService.update(familySizeId, request));
    }

    @PatchMapping("/{familySizeId}/status")
    public ApiResponse<CookFamilySizeAdminResponse> updateStatus(@PathVariable Long familySizeId,
                                                                 @Valid @RequestBody CookFamilySizeStatusRequest request) {
        return ApiResponse.success("Cook family size status updated",
                cookFamilySizeService.updateStatus(familySizeId, request));
    }

    @DeleteMapping("/{familySizeId}")
    public ApiResponse<CookFamilySizeAdminResponse> delete(@PathVariable Long familySizeId) {
        return ApiResponse.success("Cook family size deactivated",
                cookFamilySizeService.deactivate(familySizeId));
    }
}
