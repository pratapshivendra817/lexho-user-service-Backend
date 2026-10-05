package com.lexho.user_service.website.controller;

import com.lexho.user_service.admin.dto.AdminMessageResponse;
import com.lexho.user_service.admin.dto.DataResponse;
import com.lexho.user_service.website.dto.*;
import com.lexho.user_service.website.service.WebsiteHeaderService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/admin/website", "/api/v1/admin/website"})
@PreAuthorize("hasRole('ADMIN')")
public class AdminWebsiteHeaderController {
    private final WebsiteHeaderService websiteHeaderService;

    public AdminWebsiteHeaderController(WebsiteHeaderService websiteHeaderService) {
        this.websiteHeaderService = websiteHeaderService;
    }

    @GetMapping("/header/services/categories")
    public DataResponse<List<AdminWebsiteCategoryResponse>> categories() {
        return new DataResponse<>(websiteHeaderService.adminCategories());
    }

    @PatchMapping("/header/services/categories/{categoryId}/visibility")
    public DataResponse<AdminWebsiteCategoryResponse> categoryVisibility(
            @PathVariable Long categoryId,
            @Valid @RequestBody WebsiteVisibilityRequest request) {
        return new DataResponse<>(websiteHeaderService.updateCategoryVisibility(categoryId, request));
    }

    @PatchMapping("/header/services/categories/reorder")
    public AdminMessageResponse reorderCategories(@Valid @RequestBody WebsiteReorderRequest request) {
        return websiteHeaderService.reorderCategories(request);
    }

    @GetMapping("/header/cities")
    public DataResponse<List<AdminWebsiteCityResponse>> cities() {
        return new DataResponse<>(websiteHeaderService.adminCities());
    }

    @PostMapping("/header/cities")
    public DataResponse<AdminWebsiteCityResponse> createCity(@Valid @RequestBody WebsiteCityUpsertRequest request) {
        return new DataResponse<>(websiteHeaderService.createCity(request));
    }

    @PutMapping("/header/cities/{cityId}")
    public DataResponse<AdminWebsiteCityResponse> updateCity(@PathVariable Long cityId,
                                                             @Valid @RequestBody WebsiteCityUpsertRequest request) {
        return new DataResponse<>(websiteHeaderService.updateCity(cityId, request));
    }

    @PatchMapping("/header/cities/{cityId}/visibility")
    public DataResponse<AdminWebsiteCityResponse> cityVisibility(
            @PathVariable Long cityId,
            @Valid @RequestBody WebsiteVisibilityRequest request) {
        return new DataResponse<>(websiteHeaderService.updateCityVisibility(cityId, request));
    }

    @PatchMapping("/header/cities/{cityId}/status")
    public DataResponse<AdminWebsiteCityResponse> cityStatus(@PathVariable Long cityId,
                                                             @Valid @RequestBody WebsiteStatusRequest request) {
        return new DataResponse<>(websiteHeaderService.updateCityStatus(cityId, request));
    }

    @DeleteMapping("/header/cities/{cityId}")
    public AdminMessageResponse deleteCity(@PathVariable Long cityId) {
        return websiteHeaderService.deleteCity(cityId);
    }

    @PatchMapping("/header/cities/reorder")
    public AdminMessageResponse reorderCities(@Valid @RequestBody WebsiteReorderRequest request) {
        return websiteHeaderService.reorderCities(request);
    }

    @GetMapping("/cities/{cityId}/areas")
    public DataResponse<List<WebsiteAreaResponse>> areas(@PathVariable Long cityId) {
        return new DataResponse<>(websiteHeaderService.adminAreas(cityId));
    }

    @PostMapping("/cities/{cityId}/areas")
    public DataResponse<WebsiteAreaResponse> createArea(@PathVariable Long cityId,
                                                        @Valid @RequestBody WebsiteAreaUpsertRequest request) {
        return new DataResponse<>(websiteHeaderService.createArea(cityId, request));
    }

    @PutMapping("/areas/{areaId}")
    public DataResponse<WebsiteAreaResponse> updateArea(@PathVariable Long areaId,
                                                        @Valid @RequestBody WebsiteAreaUpsertRequest request) {
        return new DataResponse<>(websiteHeaderService.updateArea(areaId, request));
    }

    @PatchMapping("/areas/{areaId}/status")
    public DataResponse<WebsiteAreaResponse> areaStatus(@PathVariable Long areaId,
                                                        @Valid @RequestBody WebsiteStatusRequest request) {
        return new DataResponse<>(websiteHeaderService.updateAreaStatus(areaId, request));
    }

    @DeleteMapping("/areas/{areaId}")
    public AdminMessageResponse deleteArea(@PathVariable Long areaId) {
        return websiteHeaderService.deleteArea(areaId);
    }

    @PatchMapping("/cities/{cityId}/areas/reorder")
    public AdminMessageResponse reorderAreas(@PathVariable Long cityId,
                                             @Valid @RequestBody WebsiteReorderRequest request) {
        return websiteHeaderService.reorderAreas(cityId, request);
    }
}
