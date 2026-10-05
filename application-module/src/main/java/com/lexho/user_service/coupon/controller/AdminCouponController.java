package com.lexho.user_service.coupon.controller;

import com.lexho.user_service.coupon.dto.CouponPageResponse;
import com.lexho.user_service.coupon.dto.CouponRequest;
import com.lexho.user_service.coupon.dto.CouponResponse;
import com.lexho.user_service.coupon.dto.CouponStatusRequest;
import com.lexho.user_service.coupon.service.CouponService;
import com.lexho.user_service.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/admin/coupons", "/api/v1/admin/coupons"})
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCouponController {
    private final CouponService couponService;

    @GetMapping
    public ResponseEntity<ApiResponse<CouponPageResponse>> list(@RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "20") int size,
                                                                @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(ApiResponse.success("Coupons fetched", couponService.list(page, size, active)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CouponResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Coupon fetched", couponService.get(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CouponResponse>> create(@Valid @RequestBody CouponRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Coupon created", couponService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CouponResponse>> update(@PathVariable Long id,
                                                              @Valid @RequestBody CouponRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Coupon updated", couponService.update(id, request)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<CouponResponse>> status(@PathVariable Long id,
                                                              @Valid @RequestBody CouponStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Coupon status updated", couponService.updateStatus(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        couponService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Coupon deleted", null));
    }
}
