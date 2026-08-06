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

    private final LexhoAuthService authService;

    public LexhoAuthController(LexhoAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/otp/send")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendOtp(@Valid @RequestBody LexhoDtos.OtpSendRequest request) {
        authService.sendOtp(request.mobile(), request.role());
        return ResponseEntity.ok(ApiResponse.success("OTP sent successfully", Map.of(
                "mobile", request.mobile(),
                "resendAfter", authService.getResendAfterSeconds(),
                "role", request.role() == null ? "CUSTOMER" : request.role()
        )));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponse<LexhoDtos.AuthVerifyResponse>> verifyOtp(@Valid @RequestBody LexhoDtos.OtpVerifyRequest request) {
        LexhoDtos.AuthVerifyResponse response = authService.verifyOtp(request.mobile(), request.otp(), request.role());
        return ResponseEntity.ok(ApiResponse.success("OTP verified successfully", response, response.token()));
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
}
