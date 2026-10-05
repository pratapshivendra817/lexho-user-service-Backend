package com.lexho.user_service.lexhopartner.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.lexhopartner.dto.PartnerCatalogCategoryResponse;
import com.lexho.user_service.lexhopartner.dto.PartnerCatalogServicePageResponse;
import com.lexho.user_service.lexhopartner.dto.PartnerCatalogServiceResponse;
import com.lexho.user_service.lexhopartner.service.PartnerCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static com.lexho.user_service.lexhopartner.controller.PartnerControllerText.ATTR_USER_ID;

@RestController
@RequestMapping({"/api/partner/catalog", "/api/v1/partner/catalog"})
@PreAuthorize("hasAnyRole('PARTNER','ADMIN')")
@Tag(name = "Lexho Partner Catalog")
public class PartnerCatalogController {

    private final PartnerCatalogService partnerCatalogService;

    public PartnerCatalogController(PartnerCatalogService partnerCatalogService) {
        this.partnerCatalogService = partnerCatalogService;
    }

    @GetMapping("/categories")
    @Operation(summary = "Get active marketplace categories for partner")
    public ApiResponse<List<PartnerCatalogCategoryResponse>> categories(@RequestAttribute(ATTR_USER_ID) Long partnerId,
                                                                        @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.success(PartnerControllerText.MSG_CATEGORIES_FETCHED,
                partnerCatalogService.categories(partnerId, limit));
    }

    @GetMapping("/service-types")
    @Operation(summary = "Get active service type categories for partners")
    public ApiResponse<Map<String, List<PartnerCatalogCategoryResponse>>> serviceTypes(
            @RequestAttribute(ATTR_USER_ID) Long partnerId,
            @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.success(PartnerControllerText.MSG_SERVICE_TYPES_FETCHED,
                partnerCatalogService.categoryDropdown(partnerId, limit));
    }

    @GetMapping("/services")
    @Operation(summary = "Get active marketplace services for partner")
    public ApiResponse<PartnerCatalogServicePageResponse> services(@RequestAttribute(ATTR_USER_ID) Long partnerId,
                                                                   @RequestParam(required = false) Long categoryId,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "20") int size,
                                                                   @RequestParam(required = false) String search) {
        return ApiResponse.success(PartnerControllerText.MSG_SERVICES_FETCHED,
                partnerCatalogService.services(partnerId, categoryId, page, size, search));
    }

    @GetMapping("/services/{serviceId}")
    @Operation(summary = "Get active marketplace service detail for partner")
    public ApiResponse<PartnerCatalogServiceResponse> service(@RequestAttribute(ATTR_USER_ID) Long partnerId,
                                                              @PathVariable Long serviceId) {
        return ApiResponse.success(PartnerControllerText.MSG_SERVICE_FETCHED,
                partnerCatalogService.service(partnerId, serviceId));
    }
}
