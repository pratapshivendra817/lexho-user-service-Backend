package com.lexho.user_service.website.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.website.dto.WebsiteHeaderItemResponse;
import com.lexho.user_service.website.dto.WebsiteHeaderServiceResponse;
import com.lexho.user_service.website.service.WebsiteHeaderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/website")
public class PublicWebsiteHeaderController {
    private final WebsiteHeaderService websiteHeaderService;

    public PublicWebsiteHeaderController(WebsiteHeaderService websiteHeaderService) {
        this.websiteHeaderService = websiteHeaderService;
    }

    @GetMapping("/services/categories")
    public ApiResponse<List<WebsiteHeaderItemResponse>> categories() {
        return ApiResponse.success("Website categories fetched", websiteHeaderService.publicCategories());
    }

    @GetMapping("/services/categories/{categorySlug}/services")
    public ApiResponse<List<WebsiteHeaderServiceResponse>> services(@PathVariable String categorySlug) {
        return ApiResponse.success("Website category services fetched",
                websiteHeaderService.publicServicesByCategorySlug(categorySlug));
    }

    @GetMapping("/cities")
    public ApiResponse<List<WebsiteHeaderItemResponse>> cities() {
        return ApiResponse.success("Website cities fetched", websiteHeaderService.publicCities());
    }

    @GetMapping("/cities/{citySlug}/areas")
    public ApiResponse<List<WebsiteHeaderItemResponse>> areas(@PathVariable String citySlug) {
        return ApiResponse.success("Website city areas fetched", websiteHeaderService.publicAreasByCitySlug(citySlug));
    }
}
