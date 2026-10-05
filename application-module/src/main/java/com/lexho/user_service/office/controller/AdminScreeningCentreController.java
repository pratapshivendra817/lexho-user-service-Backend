package com.lexho.user_service.office.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.office.dto.ScreeningCentreAdminResponse;
import com.lexho.user_service.office.dto.ScreeningCentreRequest;
import com.lexho.user_service.office.dto.ScreeningCentreStatusRequest;
import com.lexho.user_service.office.service.ScreeningCentreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/admin/screening-centre", "/api/v1/admin/screening-centre"})
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Screening Centre")
public class AdminScreeningCentreController {

    private final ScreeningCentreService screeningCentreService;

    public AdminScreeningCentreController(ScreeningCentreService screeningCentreService) {
        this.screeningCentreService = screeningCentreService;
    }

    @GetMapping
    @Operation(summary = "Admin: get active Lexho screening centre")
    public ApiResponse<ScreeningCentreAdminResponse> get() {
        return ApiResponse.success("Screening centre fetched", screeningCentreService.getAdminScreeningCentre());
    }

    @PutMapping
    @Operation(summary = "Admin: create or replace active Lexho screening centre")
    public ApiResponse<ScreeningCentreAdminResponse> save(@RequestAttribute(value = "userId", required = false) Long adminId,
                                                          @Valid @RequestBody ScreeningCentreRequest request) {
        return ApiResponse.success("Screening centre saved", screeningCentreService.saveScreeningCentre(request, adminId));
    }

    @PatchMapping("/status")
    @Operation(summary = "Admin: activate or deactivate Lexho screening centre")
    public ApiResponse<ScreeningCentreAdminResponse> status(@RequestAttribute(value = "userId", required = false) Long adminId,
                                                            @Valid @RequestBody ScreeningCentreStatusRequest request) {
        return ApiResponse.success("Screening centre status updated",
                screeningCentreService.updateStatus(request.active(), adminId));
    }
}
