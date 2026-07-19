package com.lexho.user_service.customer.location.service;

import com.lexho.user_service.customer.location.dto.ReverseGeocodeResponse;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class ReverseGeocodePipelineService {

    private final ReverseGeocodeService reverseGeocodeService;

    public ReverseGeocodePipelineService(ReverseGeocodeService reverseGeocodeService) {
        this.reverseGeocodeService = reverseGeocodeService;
    }

    @Async("locationTaskExecutor")
    public CompletableFuture<ReverseGeocodeResponse> reverseAsync(double latitude, double longitude) {
        return CompletableFuture.completedFuture(reverseGeocodeService.reverse(latitude, longitude));
    }
}
