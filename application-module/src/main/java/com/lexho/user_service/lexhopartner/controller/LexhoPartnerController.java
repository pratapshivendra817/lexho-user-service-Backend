package com.lexho.user_service.lexhopartner.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.lexhopartner.dto.PartnerDocumentDto;
import com.lexho.user_service.lexhopartner.dto.PartnerProfileDto;
import com.lexho.user_service.lexhopartner.dto.PartnerAvailabilityResponse;
import com.lexho.user_service.lexhopartner.dto.PartnerBannerPageResponse;
import com.lexho.user_service.lexhopartner.dto.PartnerEarningTransactionDto;
import com.lexho.user_service.lexhopartner.dto.PartnerEarningsSummaryDto;
import com.lexho.user_service.lexhopartner.dto.PageResponseDto;
import com.lexho.user_service.lexhopartner.dto.UpdateAvailabilityRequest;
import com.lexho.user_service.lexhopartner.dto.UpdatePartnerProfileRequest;
import com.lexho.user_service.lexhopartner.dto.DashboardStatsDto;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.lexhopartner.service.PartnerBannerService;
import com.lexho.user_service.lexhopartner.service.PartnerProfileService;
import com.lexho.user_service.lexhopartner.service.PartnerOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.time.LocalDate;

@RestController
@RequestMapping({"/api/partner", "/api/v1/partner"})
@PreAuthorize("hasAnyRole('PARTNER','ADMIN')")
@Tag(name = "Lexho Partner")
public class LexhoPartnerController {

    private final PartnerProfileService partnerProfileService;
    private final PartnerOrderService partnerOrderService;
    private final PartnerBannerService partnerBannerService;

    public LexhoPartnerController(PartnerProfileService partnerProfileService,
                                  PartnerOrderService partnerOrderService,
                                  PartnerBannerService partnerBannerService) {
        this.partnerProfileService = partnerProfileService;
        this.partnerOrderService = partnerOrderService;
        this.partnerBannerService = partnerBannerService;
    }

    @GetMapping("/profile")
    @Operation(summary = "Get partner profile")
    public ApiResponse<PartnerProfileDto> profile(@RequestAttribute("userId") Long partnerId) {
        return ApiResponse.success("Profile fetched", partnerProfileService.getProfile(partnerId));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update partner profile")
    public ApiResponse<PartnerProfileDto> update(@RequestAttribute("userId") Long partnerId,
                                                 @Valid @RequestBody UpdatePartnerProfileRequest request) {
        return ApiResponse.success("Profile updated", partnerProfileService.updateProfile(partnerId, request));
    }

    @PostMapping("/profile-image")
    @Operation(summary = "Upload profile image")
    public ApiResponse<PartnerProfileDto> profileImage(@RequestAttribute("userId") Long partnerId,
                                                       @RequestPart("file") MultipartFile file) {
        return ApiResponse.success("Profile image uploaded", partnerProfileService.updateProfileImage(partnerId, file));
    }

    @PostMapping("/documents")
    @Operation(summary = "Upload KYC documents")
    public ApiResponse<List<PartnerDocumentDto>> documents(@RequestAttribute("userId") Long partnerId,
                                                           @RequestPart("aadhaar") MultipartFile aadhaar,
                                                           @RequestPart("pan") MultipartFile pan,
                                                           @RequestPart("selfie") MultipartFile selfie,
                                                           @RequestPart(value = "certificate", required = false) MultipartFile certificate) {
        return ApiResponse.success("Documents uploaded",
                partnerProfileService.uploadDocuments(partnerId, aadhaar, pan, selfie, certificate));
    }

    @PutMapping("/availability")
    @Operation(summary = "Update partner availability")
    public PartnerAvailabilityResponse availability(@RequestAttribute("userId") Long partnerId,
                                                    @Valid @RequestBody UpdateAvailabilityRequest request) {
        return partnerProfileService.updateAvailability(partnerId, request);
    }

    @GetMapping("/dashboard/stats")
    @Operation(summary = "Get partner dashboard stats")
    public DashboardStatsDto dashboardStats(@RequestAttribute("userId") Long partnerId) {
        return partnerOrderService.dashboard(partnerId);
    }

    @GetMapping("/banners")
    @Operation(summary = "Get partner home banners")
    public PartnerBannerPageResponse banners(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "5") int size) {
        return partnerBannerService.partnerBanners(page, size);
    }

    @GetMapping("/earnings/summary")
    @Operation(summary = "Get partner earnings summary")
    public ApiResponse<PartnerEarningsSummaryDto> earningsSummary(@RequestAttribute("userId") Long partnerId) {
        return ApiResponse.success("Earnings summary fetched", partnerOrderService.earningsSummary(partnerId));
    }

    @GetMapping("/earnings/transactions")
    @Operation(summary = "Get partner earning transactions")
    public ApiResponse<PageResponseDto<PartnerEarningTransactionDto>> earningTransactions(
            @RequestAttribute("userId") Long partnerId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        LocalDate from = parseDate(fromDate);
        LocalDate to = parseDate(toDate);
        return ApiResponse.success("Earning transactions fetched",
                partnerOrderService.earningTransactions(partnerId, from, to, page, size, sortBy, direction));
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception ex) {
            throw new BadRequestException("Invalid date format. Use YYYY-MM-DD");
        }
    }
}
