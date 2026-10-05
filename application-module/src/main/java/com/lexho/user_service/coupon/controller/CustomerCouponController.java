package com.lexho.user_service.coupon.controller;

import com.lexho.user_service.coupon.dto.CouponPageResponse;
import com.lexho.user_service.coupon.dto.CouponValidationRequest;
import com.lexho.user_service.coupon.dto.CouponValidationResponse;
import com.lexho.user_service.coupon.service.CouponService;
import com.lexho.user_service.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping({"/api/coupons", "/api/v1/coupons"})
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
public class CustomerCouponController {
    private final CouponService couponService;

    @GetMapping("/my-rewards")
    public ResponseEntity<ApiResponse<CouponPageResponse>> myRewards(@RequestAttribute("userId") Long userId,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("Reward coupons fetched",
                couponService.listMyRewardCoupons(userId, page, size)));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<CouponPageResponse>> available(@RequestAttribute("userId") Long userId,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "20") int size,
                                                                     @RequestParam(required = false) BigDecimal amount,
                                                                     @RequestParam(required = false) Long planId,
                                                                     @RequestParam(required = false) Long serviceId,
                                                                     @RequestParam(required = false) Long categoryId,
                                                                     @RequestParam(required = false) String bookingType) {
        Long resolvedServiceId = serviceId == null ? planId : serviceId;
        return ResponseEntity.ok(ApiResponse.success("Available coupons fetched",
                couponService.listAvailableCoupons(userId, page, size, amount, resolvedServiceId, categoryId, bookingType)));
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<CouponValidationResponse>> validate(@RequestAttribute("userId") Long userId,
                                                                          @Valid @RequestBody CouponValidationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Coupon validated", couponService.validate(userId, request)));
    }
}
