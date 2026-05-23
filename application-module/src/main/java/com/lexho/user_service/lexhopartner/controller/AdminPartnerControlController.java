package com.lexho.user_service.lexhopartner.controller;

import com.lexho.user_service.lexhopartner.dto.DashboardStatsDto;
import com.lexho.user_service.lexhopartner.dto.PartnerAvailabilityResponse;
import com.lexho.user_service.lexhopartner.dto.PartnerBannerPageResponse;
import com.lexho.user_service.lexhopartner.dto.UpdateAvailabilityRequest;
import com.lexho.user_service.dispatch.dto.PartnerSkillsUpdateRequest;
import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.lexhopartner.service.AdminPartnerGovernanceService;
import com.lexho.user_service.lexhopartner.service.PartnerBannerService;
import com.lexho.user_service.lexhopartner.service.PartnerOrderService;
import com.lexho.user_service.lexhopartner.service.PartnerProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping({"/api/admin/partners", "/api/v1/admin/partners"})
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Partner Control")
public class AdminPartnerControlController {

    private final PartnerProfileService partnerProfileService;
    private final PartnerOrderService partnerOrderService;
    private final PartnerBannerService partnerBannerService;
    private final AdminPartnerGovernanceService adminPartnerGovernanceService;

    public AdminPartnerControlController(PartnerProfileService partnerProfileService,
                                         PartnerOrderService partnerOrderService,
                                         PartnerBannerService partnerBannerService,
                                         AdminPartnerGovernanceService adminPartnerGovernanceService) {
        this.partnerProfileService = partnerProfileService;
        this.partnerOrderService = partnerOrderService;
        this.partnerBannerService = partnerBannerService;
        this.adminPartnerGovernanceService = adminPartnerGovernanceService;
    }

    @GetMapping("/{partnerId}/dashboard/stats")
    @Operation(summary = "Get dashboard stats for a specific partner")
    public DashboardStatsDto dashboardStats(@PathVariable Long partnerId) {
        return partnerOrderService.dashboard(partnerId);
    }

    @PutMapping("/{partnerId}/availability")
    @Operation(summary = "Update availability for a specific partner")
    public PartnerAvailabilityResponse updateAvailability(@PathVariable Long partnerId,
                                                          @Valid @RequestBody UpdateAvailabilityRequest request) {
        return partnerProfileService.updateAvailability(partnerId, request);
    }

    @GetMapping("/{partnerId}/banners")
    @Operation(summary = "Preview partner banner feed")
    public PartnerBannerPageResponse partnerBanners(@PathVariable Long partnerId,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "5") int size) {
        return partnerBannerService.partnerBanners(page, size);
    }

    @GetMapping("/settlements")
    @Operation(summary = "List partner settlements")
    public ApiResponse<Map<String, Object>> settlements(@RequestParam(required = false) String search,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success("Settlements fetched",
                adminPartnerGovernanceService.settlements(search, status, page, size));
    }

    @PatchMapping("/settlements/{payoutId}/status")
    @Operation(summary = "Update settlement status")
    public ApiResponse<Map<String, Object>> updateSettlementStatus(@PathVariable Long payoutId,
                                                                   @RequestBody SettlementStatusRequest request) {
        return ApiResponse.success("Settlement status updated",
                adminPartnerGovernanceService.updateSettlementStatus(payoutId, request.status, request.remarks));
    }

    @PatchMapping("/{partnerId}/kyc/documents/{documentId}/review")
    @Operation(summary = "Review partner KYC document")
    public ApiResponse<Map<String, Object>> reviewKycDocument(@PathVariable Long partnerId,
                                                               @PathVariable Long documentId,
                                                               @RequestAttribute(value = "userId", required = false) Long adminId,
                                                               @RequestBody KycDecisionRequest request) {
        return ApiResponse.success("Document reviewed",
                adminPartnerGovernanceService.reviewKycDocument(partnerId, documentId, request.decision, request.reason, adminId));
    }

    @PatchMapping("/{partnerId}/availability/force")
    @Operation(summary = "Force partner availability and optional lock")
    public ApiResponse<Map<String, Object>> forceAvailability(@PathVariable Long partnerId,
                                                              @RequestBody ForceAvailabilityRequest request) {
        return ApiResponse.success("Partner availability overridden",
                adminPartnerGovernanceService.forceAvailability(partnerId, request.online, request.lockMinutes, request.reason));
    }

    @PutMapping("/{partnerId}/skills/governance")
    @Operation(summary = "Admin skill governance for partner")
    public ApiResponse<Map<String, Object>> governSkills(@PathVariable Long partnerId,
                                                         @Valid @RequestBody PartnerSkillsUpdateRequest request) {
        return ApiResponse.success("Partner skills governed",
                adminPartnerGovernanceService.governSkills(partnerId, request));
    }

    @PatchMapping("/{partnerId}/risk/action")
    @Operation(summary = "Fraud and risk action")
    public ApiResponse<Map<String, Object>> riskAction(@PathVariable Long partnerId,
                                                       @RequestBody RiskActionRequest request) {
        return ApiResponse.success("Risk action applied",
                adminPartnerGovernanceService.riskAction(partnerId, request.action, request.reason));
    }

    public static class SettlementStatusRequest {
        public String status;
        public String remarks;
    }

    public static class KycDecisionRequest {
        public String decision;
        public String reason;
    }

    public static class ForceAvailabilityRequest {
        public boolean online;
        public Integer lockMinutes;
        public String reason;
    }

    public static class RiskActionRequest {
        public String action;
        public String reason;
    }
}
