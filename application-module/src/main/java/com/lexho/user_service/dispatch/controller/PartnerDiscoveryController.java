package com.lexho.user_service.dispatch.controller;

import com.lexho.user_service.dispatch.dto.PartnerNearbyRequest;
import com.lexho.user_service.dispatch.dto.PartnerNearbyResponse;
import com.lexho.user_service.dispatch.service.PartnerNearbyService;
import com.lexho.user_service.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/partners")
@PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
public class PartnerDiscoveryController {

    private final PartnerNearbyService partnerNearbyService;

    public PartnerDiscoveryController(PartnerNearbyService partnerNearbyService) {
        this.partnerNearbyService = partnerNearbyService;
    }

    @PostMapping("/nearby")
    public ApiResponse<PartnerNearbyResponse> nearby(@Valid @RequestBody PartnerNearbyRequest request) {
        return ApiResponse.success("Nearby partners fetched", partnerNearbyService.findNearby(request));
    }
}

