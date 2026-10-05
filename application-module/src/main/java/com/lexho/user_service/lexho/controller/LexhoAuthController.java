package com.lexho.user_service.lexho.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.lexho.dto.LexhoDtos;
import com.lexho.user_service.lexho.service.LexhoAuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class LexhoAuthController {
    private static final String ROLE_CUSTOMER = "CUSTOMER";
    private static final String ROLE_PARTNER = "PARTNER";

    private final LexhoAuthService authService;

    public LexhoAuthController(LexhoAuthService authService) {
        this.authService = authService;
    }

    @PostMapping({"/otp/send", "/api/otp/send", "/api/v1/otp/send"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendOtp(@Valid @RequestBody LexhoDtos.OtpSendRequest request) {
        String role = normalizeRole(request.role());
        authService.sendOtp(request.mobile(), role);
        return ResponseEntity.ok(ApiResponse.success("OTP sent successfully", Map.of(
                "mobile", request.mobile(),
                "resendAfter", authService.getResendAfterSeconds(),
                "role", role
        )));
    }

    @PostMapping({"/otp/verify", "/api/otp/verify", "/api/v1/otp/verify"})
    public ResponseEntity<ApiResponse<LexhoDtos.AuthVerifyResponse>> verifyOtp(@Valid @RequestBody LexhoDtos.OtpVerifyRequest request) {
        LexhoDtos.AuthVerifyResponse response = authService.verifyOtp(request.mobile(), request.otp(), normalizeRole(request.role()));
        return ResponseEntity.ok(ApiResponse.success("OTP verified successfully", response, response.token()));
    }

    @PostMapping({
            "/customer/otp/send",
            "/api/customer/otp/send",
            "/api/v1/customer/otp/send",
            "/api/auth/customer/otp/send",
            "/api/v1/auth/customer/otp/send"
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendCustomerOtp(
            @Valid @RequestBody LexhoDtos.OtpSendRequest request) {
        return sendOtpForRole(request.mobile(), ROLE_CUSTOMER);
    }

    @PostMapping({
            "/customer/otp/verify",
            "/api/customer/otp/verify",
            "/api/v1/customer/otp/verify",
            "/api/auth/customer/otp/verify",
            "/api/v1/auth/customer/otp/verify"
    })
    public ResponseEntity<ApiResponse<LexhoDtos.AuthVerifyResponse>> verifyCustomerOtp(
            @Valid @RequestBody LexhoDtos.OtpVerifyRequest request) {
        return verifyOtpForRole(request.mobile(), request.otp(), ROLE_CUSTOMER);
    }

    @PostMapping({
            "/partner/otp/send",
            "/api/partner/otp/send",
            "/api/v1/partner/otp/send",
            "/api/auth/partner/otp/send",
            "/api/v1/auth/partner/otp/send"
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendPartnerOtp(
            @Valid @RequestBody LexhoDtos.OtpSendRequest request) {
        return sendOtpForRole(request.mobile(), ROLE_PARTNER);
    }

    @PostMapping({
            "/partner/otp/verify",
            "/api/partner/otp/verify",
            "/api/v1/partner/otp/verify",
            "/api/auth/partner/otp/verify",
            "/api/v1/auth/partner/otp/verify"
    })
    public ResponseEntity<ApiResponse<LexhoDtos.AuthVerifyResponse>> verifyPartnerOtp(
            @Valid @RequestBody LexhoDtos.OtpVerifyRequest request) {
        return verifyOtpForRole(request.mobile(), request.otp(), ROLE_PARTNER);
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LexhoDtos.RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    @PostMapping("/auth/refresh-token")
    public ResponseEntity<ApiResponse<Map<String, String>>> refreshToken(@Valid @RequestBody LexhoDtos.RefreshTokenRequest request) {
        String token = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", Map.of("token", token), token));
    }

    @PostMapping({"/api/auth/refresh", "/api/v1/auth/refresh"})
    public ResponseEntity<ApiResponse<Map<String, String>>> refreshTokenApi(@Valid @RequestBody LexhoDtos.RefreshTokenApiRequest request) {
        String token = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", Map.of("accessToken", token), token));
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> sendOtpForRole(String mobile, String role) {
        authService.sendOtp(mobile, role);
        return ResponseEntity.ok(ApiResponse.success("OTP sent successfully", Map.of(
                "mobile", mobile,
                "resendAfter", authService.getResendAfterSeconds(),
                "role", role
        )));
    }

    private ResponseEntity<ApiResponse<LexhoDtos.AuthVerifyResponse>> verifyOtpForRole(
            String mobile, String otp, String role) {
        LexhoDtos.AuthVerifyResponse response = authService.verifyOtp(mobile, otp, role);
        return ResponseEntity.ok(ApiResponse.success("OTP verified successfully", response, response.token()));
    }

    private String normalizeRole(String role) {
        return role == null || role.isBlank() ? ROLE_CUSTOMER : role.trim().toUpperCase();
    }
}
