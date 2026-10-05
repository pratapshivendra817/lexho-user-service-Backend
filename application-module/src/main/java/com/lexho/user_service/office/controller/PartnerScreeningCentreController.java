package com.lexho.user_service.office.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.office.dto.ScreeningCentrePartnerResponse;
import com.lexho.user_service.office.service.ScreeningCentreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/partner/screening-centre", "/api/v1/partner/screening-centre"})
@PreAuthorize("hasRole('PARTNER')")
@Tag(name = "Partner Screening Centre")
public class PartnerScreeningCentreController {

    private final ScreeningCentreService screeningCentreService;

    public PartnerScreeningCentreController(ScreeningCentreService screeningCentreService) {
        this.screeningCentreService = screeningCentreService;
    }

    @GetMapping
    @Operation(summary = "Partner: get active Lexho screening centre navigation destination")
    public ApiResponse<ScreeningCentrePartnerResponse> get() {
        return ApiResponse.success("Screening centre fetched", screeningCentreService.getPartnerScreeningCentre());
    }
}
