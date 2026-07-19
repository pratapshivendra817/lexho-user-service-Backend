package com.lexho.user_service.subscription.controller;

import com.lexho.user_service.admin.dto.AdminMessageResponse;
import com.lexho.user_service.admin.dto.DataResponse;
import com.lexho.user_service.subscription.dto.CookSubscriptionPricingRequest;
import com.lexho.user_service.subscription.dto.CookSubscriptionPricingResponse;
import com.lexho.user_service.subscription.dto.SubscriptionDurationPriceRequest;
import com.lexho.user_service.subscription.dto.SubscriptionDurationPriceResponse;
import com.lexho.user_service.subscription.service.SubscriptionPricingService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/admin/subscription-pricing", "/api/v1/admin/subscription-pricing"})
@PreAuthorize("hasRole('ADMIN')")
public class AdminSubscriptionPricingController {

    private final SubscriptionPricingService subscriptionPricingService;

    public AdminSubscriptionPricingController(SubscriptionPricingService subscriptionPricingService) {
        this.subscriptionPricingService = subscriptionPricingService;
    }

    @GetMapping
    public DataResponse<List<SubscriptionDurationPriceResponse>> list() {
        return subscriptionPricingService.adminList();
    }

    @PostMapping
    public DataResponse<SubscriptionDurationPriceResponse> create(
            @Valid @RequestBody SubscriptionDurationPriceRequest request) {
        return subscriptionPricingService.create(request);
    }

    @PutMapping("/{id}")
    public DataResponse<SubscriptionDurationPriceResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SubscriptionDurationPriceRequest request) {
        return subscriptionPricingService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public AdminMessageResponse delete(@PathVariable Long id) {
        return subscriptionPricingService.delete(id);
    }

    @GetMapping("/cook")
    public DataResponse<List<CookSubscriptionPricingResponse>> cookList() {
        return subscriptionPricingService.adminCookList();
    }

    @PostMapping("/cook")
    public DataResponse<CookSubscriptionPricingResponse> createCook(
            @Valid @RequestBody CookSubscriptionPricingRequest request) {
        return subscriptionPricingService.createCook(request);
    }

    @PutMapping("/cook/{id}")
    public DataResponse<CookSubscriptionPricingResponse> updateCook(
            @PathVariable Long id,
            @Valid @RequestBody CookSubscriptionPricingRequest request) {
        return subscriptionPricingService.updateCook(id, request);
    }

    @DeleteMapping("/cook/{id}")
    public AdminMessageResponse deleteCook(@PathVariable Long id) {
        return subscriptionPricingService.deleteCook(id);
    }
}
