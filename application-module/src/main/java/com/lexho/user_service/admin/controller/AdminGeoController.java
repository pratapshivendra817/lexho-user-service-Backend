package com.lexho.user_service.admin.controller;

import com.lexho.user_service.admin.geo.dto.CityResponse;
import com.lexho.user_service.admin.geo.dto.CityUpsertRequest;
import com.lexho.user_service.admin.geo.dto.ZoneResponse;
import com.lexho.user_service.admin.geo.dto.ZoneUpsertRequest;
import com.lexho.user_service.admin.geo.service.AdminGeoService;
import com.lexho.user_service.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/admin/geo", "/api/v1/admin/geo"})
@PreAuthorize("hasRole('ADMIN')")
public class AdminGeoController {

    private final AdminGeoService adminGeoService;

    public AdminGeoController(AdminGeoService adminGeoService) {
        this.adminGeoService = adminGeoService;
    }

    @GetMapping("/cities")
    public ApiResponse<List<CityResponse>> listCities() {
        return ApiResponse.success("Cities fetched", adminGeoService.listCities());
    }

    @PostMapping("/cities")
    public ApiResponse<CityResponse> createCity(@Valid @RequestBody CityUpsertRequest request) {
        return ApiResponse.success("City created", adminGeoService.createCity(request));
    }

    @PutMapping("/cities/{id}")
    public ApiResponse<CityResponse> updateCity(@PathVariable Long id, @Valid @RequestBody CityUpsertRequest request) {
        return ApiResponse.success("City updated", adminGeoService.updateCity(id, request));
    }

    @DeleteMapping("/cities/{id}")
    public ApiResponse<Void> deleteCity(@PathVariable Long id) {
        adminGeoService.deleteCity(id);
        return ApiResponse.success("City deleted", null);
    }

    @GetMapping("/zones")
    public ApiResponse<List<ZoneResponse>> listZones(@RequestParam(required = false) Long cityId) {
        return ApiResponse.success("Zones fetched", adminGeoService.listZones(cityId));
    }

    @PostMapping("/zones")
    public ApiResponse<ZoneResponse> createZone(@Valid @RequestBody ZoneUpsertRequest request) {
        return ApiResponse.success("Zone created", adminGeoService.createZone(request));
    }

    @PutMapping("/zones/{id}")
    public ApiResponse<ZoneResponse> updateZone(@PathVariable Long id, @Valid @RequestBody ZoneUpsertRequest request) {
        return ApiResponse.success("Zone updated", adminGeoService.updateZone(id, request));
    }

    @DeleteMapping("/zones/{id}")
    public ApiResponse<Void> deleteZone(@PathVariable Long id) {
        adminGeoService.deleteZone(id);
        return ApiResponse.success("Zone deleted", null);
    }
}
