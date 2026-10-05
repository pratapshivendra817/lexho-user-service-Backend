package com.lexho.user_service.referral.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.referral.dto.ReferralConfigurationRequest;
import com.lexho.user_service.referral.dto.ReferralConfigurationResponse;
import com.lexho.user_service.referral.service.ReferralService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/admin/referrals", "/api/v1/admin/referrals"})
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminReferralController {
    private final ReferralService referralService;

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<ReferralConfigurationResponse>> getConfiguration() {
        return ResponseEntity.ok(ApiResponse.success("Referral configuration fetched",
                referralService.getConfiguration()));
    }

    @PutMapping("/config")
    public ResponseEntity<ApiResponse<ReferralConfigurationResponse>> updateConfiguration(
            @Valid @RequestBody ReferralConfigurationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Referral configuration updated",
                referralService.updateConfiguration(request)));
    }
}
