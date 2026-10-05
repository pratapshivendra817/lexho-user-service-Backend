package com.lexho.user_service.referral.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.referral.dto.ReferralApplyRequest;
import com.lexho.user_service.referral.dto.ReferralCodeResponse;
import com.lexho.user_service.referral.dto.ReferralResponse;
import com.lexho.user_service.referral.service.ReferralService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/referrals", "/api/v1/referrals"})
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
public class CustomerReferralController {
    private final ReferralService referralService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<ReferralCodeResponse>> generate(@RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Referral generated", referralService.generateCode(userId)));
    }

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<ReferralResponse>> apply(@RequestAttribute("userId") Long userId,
                                                               @Valid @RequestBody ReferralApplyRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Referral applied", referralService.apply(userId, request)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ReferralResponse>> mine(@RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Referral fetched", referralService.myReferral(userId)));
    }
}
