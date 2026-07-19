package com.lexho.user_service.map.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.map.dto.NavigationRouteRequest;
import com.lexho.user_service.map.dto.NavigationRouteResponse;
import com.lexho.user_service.map.service.PartnerNavigationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/navigation")
public class PartnerNavigationController {

    private final PartnerNavigationService partnerNavigationService;

    public PartnerNavigationController(PartnerNavigationService partnerNavigationService) {
        this.partnerNavigationService = partnerNavigationService;
    }

    @PostMapping("/route")
    public ApiResponse<NavigationRouteResponse> route(@Valid @RequestBody NavigationRouteRequest request) {
        return ApiResponse.success("Navigation route fetched", partnerNavigationService.route(request));
    }

    @GetMapping("/eta/{bookingId}")
    public ApiResponse<NavigationRouteResponse> eta(@PathVariable Long bookingId) {
        return ApiResponse.success("Navigation ETA fetched", partnerNavigationService.etaForBooking(bookingId));
    }
}
