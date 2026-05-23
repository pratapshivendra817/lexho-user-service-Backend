package com.lexho.user_service.admin.controller;

import com.lexho.user_service.admin.dto.AdminProfileContentRequest;
import com.lexho.user_service.admin.dto.AdminProfileContentResponse;
import com.lexho.user_service.admin.service.AdminProfileContentService;
import com.lexho.user_service.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/admin/profile-content", "/api/v1/admin/profile-content"})
public class AdminProfileContentController {

    private final AdminProfileContentService adminProfileContentService;

    public AdminProfileContentController(AdminProfileContentService adminProfileContentService) {
        this.adminProfileContentService = adminProfileContentService;
    }

    @GetMapping
    public ApiResponse<AdminProfileContentResponse> getProfileContent() {
        return ApiResponse.success("ok", adminProfileContentService.getProfileContent());
    }

    @PutMapping
    public ApiResponse<AdminProfileContentResponse> updateProfileContent(@Valid @RequestBody AdminProfileContentRequest request) {
        return ApiResponse.success("ok", adminProfileContentService.updateProfileContent(request));
    }
}
