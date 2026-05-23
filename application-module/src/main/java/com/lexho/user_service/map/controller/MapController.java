package com.lexho.user_service.map.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.map.dto.MapboxGeocodeResponse;
import com.lexho.user_service.map.dto.RouteEtaRequest;
import com.lexho.user_service.map.dto.RouteEtaResponse;
import com.lexho.user_service.map.service.MapboxGeocodingService;
import com.lexho.user_service.map.service.MapNavigationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/maps", "/api/v1/maps"})
public class MapController {

    private final MapboxGeocodingService mapboxGeocodingService;
    private final MapNavigationService mapNavigationService;

    public MapController(MapboxGeocodingService mapboxGeocodingService,
                         MapNavigationService mapNavigationService) {
        this.mapboxGeocodingService = mapboxGeocodingService;
        this.mapNavigationService = mapNavigationService;
    }

    @GetMapping("/geocode")
    public ApiResponse<MapboxGeocodeResponse> geocode(@RequestParam("q") String query,
                                                       @RequestParam(value = "limit", required = false) Integer limit) {
        return ApiResponse.success("Mapbox geocode fetched", mapboxGeocodingService.forwardGeocode(query, limit));
    }

    @PostMapping("/route/eta")
    public ApiResponse<RouteEtaResponse> routeEta(@Valid @RequestBody RouteEtaRequest request) {
        return ApiResponse.success("Route ETA fetched", mapNavigationService.estimateRoute(request));
    }
}
