package com.lexho.user_service.banner.controller;

import com.lexho.user_service.banner.dto.BannerEventRequest;
import com.lexho.user_service.banner.dto.BannerEventResponse;
import com.lexho.user_service.banner.dto.BannerPromotionResponse;
import com.lexho.user_service.banner.dto.BannerRedeemRequest;
import com.lexho.user_service.banner.enums.BannerPlatformType;
import com.lexho.user_service.banner.service.CustomerBannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/banners")
@Tag(name = "Customer Banners", description = "Customer APIs for homepage banners and engagement events")
public class CustomerBannerController {

    private final CustomerBannerService customerBannerService;

    public CustomerBannerController(CustomerBannerService customerBannerService) {
        this.customerBannerService = customerBannerService;
    }

    @GetMapping("/homepage")
    @Operation(summary = "Get active homepage banners with targeting filters")
    public ResponseEntity<List<BannerPromotionResponse>> homepage(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Long serviceCategoryId,
            @RequestParam(defaultValue = "ALL") BannerPlatformType platformType,
            @RequestParam(required = false) Boolean newUser,
            @RequestParam(required = false) Boolean premiumUser,
            @RequestParam(required = false) BigDecimal orderValue,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String deviceId) {
        return ResponseEntity.ok(customerBannerService.homepageBanners(
                city, serviceCategoryId, platformType, newUser, premiumUser, orderValue, userId, deviceId
        ));
    }

    @PostMapping("/{id}/click")
    @Operation(summary = "Track banner click")
    public ResponseEntity<BannerEventResponse> click(@PathVariable Long id,
                                                     @RequestBody(required = false) BannerEventRequest request) {
        return ResponseEntity.ok(customerBannerService.trackClick(id, request));
    }

    @PostMapping("/{id}/impression")
    @Operation(summary = "Track banner impression")
    public ResponseEntity<BannerEventResponse> impression(@PathVariable Long id,
                                                          @RequestBody(required = false) BannerEventRequest request) {
        return ResponseEntity.ok(customerBannerService.trackImpression(id, request));
    }

    @PostMapping("/{id}/redeem")
    @Operation(summary = "Redeem promotion")
    public ResponseEntity<BannerEventResponse> redeem(@PathVariable Long id,
                                                      @Valid @RequestBody(required = false) BannerRedeemRequest request) {
        return ResponseEntity.ok(customerBannerService.redeem(id, request));
    }
}

