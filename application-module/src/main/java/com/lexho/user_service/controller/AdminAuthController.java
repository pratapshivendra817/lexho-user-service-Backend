package com.lexho.user_service.controller;

import com.lexho.user_service.admin.dto.AdminAccessTokenResponse;
import com.lexho.user_service.admin.dto.AdminMessageResponse;
import com.lexho.user_service.dto.AdminLoginRequest;
import com.lexho.user_service.dto.AdminTokenResponse;
import com.lexho.user_service.dto.AdminVerifyOtpRequest;
import com.lexho.user_service.service.AdminAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public AdminMessageResponse login(@Valid @RequestBody AdminLoginRequest request) {
        adminAuthService.login(request);
        return new AdminMessageResponse("OTP sent successfully");
    }

    @PostMapping("/verify-otp")
    public AdminAccessTokenResponse verifyOtp(@Valid @RequestBody AdminVerifyOtpRequest request) {
        AdminTokenResponse response = adminAuthService.verifyOtp(request);
        return new AdminAccessTokenResponse(response.getToken());
    }
}
