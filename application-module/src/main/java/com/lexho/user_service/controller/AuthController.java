package com.lexho.user_service.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.dto.OtpRequest;
import com.lexho.user_service.dto.OtpSendResponse;
import com.lexho.user_service.dto.OtpVerifyRequest;
import com.lexho.user_service.dto.OtpVerifyResponse;
import com.lexho.user_service.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/otp", "/api/auth"})
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<OtpSendResponse>> sendOtp(@Valid @RequestBody OtpRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("OTP sent successfully", authService.sendOtp(request.getMobile()))
        );
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<OtpVerifyResponse>> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("OTP verified successfully", authService.verifyOtp(request.getMobile(), request.getOtp()))
        );
    }
}
