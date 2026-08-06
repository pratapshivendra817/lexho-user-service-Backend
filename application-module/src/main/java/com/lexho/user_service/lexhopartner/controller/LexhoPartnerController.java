package com.lexho.user_service.lexhopartner.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.lexhopartner.dto.PartnerDocumentDto;
import com.lexho.user_service.lexhopartner.dto.PartnerDeviceTokenRequest;
import com.lexho.user_service.lexhopartner.dto.PartnerProfileDto;
import com.lexho.user_service.lexhopartner.dto.PartnerAvailabilityResponse;
import com.lexho.user_service.lexhopartner.dto.PartnerBannerPageResponse;
import com.lexho.user_service.lexhopartner.dto.PartnerEarningTransactionDto;
import com.lexho.user_service.lexhopartner.dto.PartnerEarningsSummaryDto;
import com.lexho.user_service.lexhopartner.dto.PartnerLeaveRequest;
import com.lexho.user_service.lexhopartner.dto.PageResponseDto;
import com.lexho.user_service.lexhopartner.dto.PartnerServiceAreaRequest;
import com.lexho.user_service.lexhopartner.dto.UpdateAvailabilityRequest;
import com.lexho.user_service.lexhopartner.dto.UpdatePartnerProfileRequest;
import com.lexho.user_service.lexhopartner.dto.DashboardStatsDto;
import com.lexho.user_service.lexhopartner.dto.CreateWithdrawalRequest;
import com.lexho.user_service.lexhopartner.dto.PartnerBankDetailsDto;
import com.lexho.user_service.lexhopartner.dto.PartnerPayoutDto;
import com.lexho.user_service.lexhopartner.dto.PartnerWarningCommentRequest;
import com.lexho.user_service.lexhopartner.dto.PartnerWarningDisputeRequest;
import com.lexho.user_service.lexhopartner.dto.PartnerWithdrawalDto;
import com.lexho.user_service.lexhopartner.dto.UpdatePartnerBankDetailsRequest;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.lexhopartner.service.PartnerActivityFeedService;
import com.lexho.user_service.lexhopartner.service.PartnerBannerService;
import com.lexho.user_service.lexhopartner.service.PartnerDeviceTokenService;
import com.lexho.user_service.lexhopartner.service.PartnerProfileService;
import com.lexho.user_service.lexhopartner.service.PartnerOrderService;
import com.lexho.user_service.lexhopartner.service.PartnerOperationsService;
import com.lexho.user_service.lexhopartner.service.PartnerSettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.time.LocalDate;

@RestController
@RequestMapping({"/api/partner", "/api/v1/partner"})
@PreAuthorize("hasAnyRole('PARTNER','ADMIN')")
@Tag(name = "Lexho Partner")
public class LexhoPartnerController {

    private final PartnerProfileService partnerProfileService;
    private final PartnerOrderService partnerOrderService;
    private final PartnerBannerService partnerBannerService;
    private final PartnerSettlementService partnerSettlementService;
    private final PartnerDeviceTokenService partnerDeviceTokenService;
    private final PartnerOperationsService partnerOperationsService;
    private final PartnerActivityFeedService partnerActivityFeedService;

    public LexhoPartnerController(PartnerProfileService partnerProfileService,
                                  PartnerOrderService partnerOrderService,
                                  PartnerBannerService partnerBannerService,
                                  PartnerSettlementService partnerSettlementService,
                                  PartnerDeviceTokenService partnerDeviceTokenService,
                                  PartnerOperationsService partnerOperationsService,
                                  PartnerActivityFeedService partnerActivityFeedService) {
        this.partnerProfileService = partnerProfileService;
        this.partnerOrderService = partnerOrderService;
        this.partnerBannerService = partnerBannerService;
        this.partnerSettlementService = partnerSettlementService;
        this.partnerDeviceTokenService = partnerDeviceTokenService;
        this.partnerOperationsService = partnerOperationsService;
        this.partnerActivityFeedService = partnerActivityFeedService;
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

    @PostMapping("/device-token")
    @Operation(summary = "Register partner FCM device token")
    public ApiResponse<Map<String, Object>> registerDeviceToken(@RequestAttribute("userId") Long partnerId,
                                                                @Valid @RequestBody PartnerDeviceTokenRequest request) {
        return ApiResponse.success("Device token registered", partnerDeviceTokenService.register(partnerId, request));
    }

    @DeleteMapping("/device-token")
    @Operation(summary = "Deactivate partner FCM device token")
    public ApiResponse<Map<String, Object>> deleteDeviceToken(@RequestAttribute("userId") Long partnerId,
                                                              @Valid @RequestBody PartnerDeviceTokenRequest request) {
        return ApiResponse.success("Device token deleted", partnerDeviceTokenService.delete(partnerId, request));
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

    @GetMapping("/payouts")
    @Operation(summary = "Get partner payouts")
    public ApiResponse<PageResponseDto<PartnerPayoutDto>> payouts(@RequestAttribute("userId") Long partnerId,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success("Payouts fetched", partnerSettlementService.payouts(partnerId, page, size));
    }

    @GetMapping("/performance")
    @Operation(summary = "Get partner performance metrics")
    public ApiResponse<Map<String, Object>> performance(@RequestAttribute("userId") Long partnerId) {
        return ApiResponse.success("Performance fetched", partnerOperationsService.performance(partnerId));
    }

    @GetMapping("/ratings")
    @Operation(summary = "Get partner ratings and reviews")
    public ApiResponse<Map<String, Object>> ratings(@RequestAttribute("userId") Long partnerId,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success("Ratings fetched", partnerOperationsService.ratings(partnerId, page, size));
    }

    @PostMapping("/leave")
    @Operation(summary = "Create partner leave")
    public ApiResponse<Map<String, Object>> createLeave(@RequestAttribute("userId") Long partnerId,
                                                        @Valid @RequestBody PartnerLeaveRequest request) {
        return ApiResponse.success("Leave created", partnerOperationsService.createLeave(partnerId, request));
    }

    @PatchMapping("/leave/{id}/cancel")
    @Operation(summary = "Cancel partner leave")
    public ApiResponse<Map<String, Object>> cancelLeave(@RequestAttribute("userId") Long partnerId,
                                                        @PathVariable Long id) {
        return ApiResponse.success("Leave cancelled", partnerOperationsService.cancelLeave(partnerId, id));
    }

    @GetMapping("/leave")
    @Operation(summary = "Get partner leave history")
    public ApiResponse<List<Map<String, Object>>> leaves(@RequestAttribute("userId") Long partnerId) {
        return ApiResponse.success("Leaves fetched", partnerOperationsService.leaves(partnerId));
    }

    @GetMapping("/service-areas")
    @Operation(summary = "Get partner service area preferences")
    public ApiResponse<List<Map<String, Object>>> serviceAreas(@RequestAttribute("userId") Long partnerId) {
        return ApiResponse.success("Service areas fetched", partnerOperationsService.serviceAreas(partnerId));
    }

    @PutMapping("/service-areas")
    @Operation(summary = "Update partner service area preferences")
    public ApiResponse<List<Map<String, Object>>> updateServiceAreas(@RequestAttribute("userId") Long partnerId,
                                                                     @Valid @RequestBody PartnerServiceAreaRequest request) {
        return ApiResponse.success("Service areas updated", partnerOperationsService.updateServiceAreas(partnerId, request));
    }

    @PatchMapping("/service-areas")
    @Operation(summary = "Update partner service area preferences using selected zones")
    public ApiResponse<List<Map<String, Object>>> patchServiceAreas(@RequestAttribute("userId") Long partnerId,
                                                                    @RequestBody List<PartnerServiceAreaRequest.ServiceAreaItem> areas) {
        return ApiResponse.success("Service areas updated", partnerOperationsService.updateServiceAreas(partnerId, areas));
    }

    @GetMapping("/warnings")
    @Operation(summary = "Get partner warnings")
    public ApiResponse<List<Map<String, Object>>> warnings(@RequestAttribute("userId") Long partnerId) {
        return ApiResponse.success("Warnings fetched", partnerOperationsService.warnings(partnerId));
    }

    @GetMapping("/warnings/{warningId}")
    @Operation(summary = "Get partner warning details")
    public ApiResponse<Map<String, Object>> warning(@RequestAttribute("userId") Long partnerId,
                                                    @PathVariable Long warningId) {
        return ApiResponse.success("Warning fetched", partnerOperationsService.warningDetail(partnerId, warningId));
    }

    @PatchMapping("/warnings/{warningId}/acknowledge")
    @Operation(summary = "Acknowledge partner warning")
    public ApiResponse<Map<String, Object>> acknowledgeWarning(@RequestAttribute("userId") Long partnerId,
                                                               @PathVariable Long warningId) {
        return ApiResponse.success("Warning acknowledged",
                partnerOperationsService.acknowledgeWarning(partnerId, warningId));
    }

    @PostMapping("/warnings/{warningId}/dispute")
    @Operation(summary = "Dispute partner warning")
    public ApiResponse<Map<String, Object>> disputeWarning(@RequestAttribute("userId") Long partnerId,
                                                           @PathVariable Long warningId,
                                                           @Valid @RequestBody PartnerWarningDisputeRequest request) {
        return ApiResponse.success("Warning dispute submitted",
                partnerOperationsService.disputeWarning(partnerId, warningId, request.reason(), request.attachmentUrls()));
    }

    @PostMapping("/warnings/{warningId}/comments")
    @Operation(summary = "Add warning dispute comment")
    public ApiResponse<Map<String, Object>> warningComment(@RequestAttribute("userId") Long partnerId,
                                                           @PathVariable Long warningId,
                                                           @Valid @RequestBody PartnerWarningCommentRequest request) {
        return ApiResponse.success("Warning comment added",
                partnerOperationsService.addWarningComment(partnerId, warningId, request));
    }

    @GetMapping("/warnings/{warningId}/stream")
    @Operation(summary = "Stream warning updates")
    public SseEmitter warningStream(@RequestAttribute("userId") Long partnerId,
                                    @PathVariable Long warningId) {
        return partnerOperationsService.warningStream(partnerId, warningId);
    }

    @GetMapping("/payouts/{payoutId}")
    @Operation(summary = "Get partner payout details")
    public ApiResponse<PartnerPayoutDto> payout(@RequestAttribute("userId") Long partnerId,
                                                @PathVariable Long payoutId) {
        return ApiResponse.success("Payout fetched", partnerSettlementService.payout(partnerId, payoutId));
    }

    @GetMapping("/bank-details")
    @Operation(summary = "Get masked partner bank details")
    public ApiResponse<PartnerBankDetailsDto> bankDetails(@RequestAttribute("userId") Long partnerId) {
        return ApiResponse.success("Bank details fetched", partnerProfileService.bankDetails(partnerId));
    }

    @PutMapping("/bank-details")
    @Operation(summary = "Update partner bank details")
    public ApiResponse<PartnerBankDetailsDto> updateBankDetails(@RequestAttribute("userId") Long partnerId,
                                                                @Valid @RequestBody UpdatePartnerBankDetailsRequest request) {
        return ApiResponse.success("Bank details updated", partnerProfileService.updateBankDetails(partnerId, request));
    }

    @PostMapping("/withdrawals")
    @Operation(summary = "Request withdrawal")
    public ApiResponse<PartnerWithdrawalDto> requestWithdrawal(@RequestAttribute("userId") Long partnerId,
                                                                @Valid @RequestBody CreateWithdrawalRequest request) {
        return ApiResponse.success("Withdrawal requested",
                partnerSettlementService.requestWithdrawal(partnerId, request));
    }

    @GetMapping("/withdrawals")
    @Operation(summary = "Get partner withdrawal requests")
    public ApiResponse<PageResponseDto<PartnerWithdrawalDto>> withdrawals(@RequestAttribute("userId") Long partnerId,
                                                                          @RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success("Withdrawals fetched", partnerSettlementService.withdrawals(partnerId, page, size));
    }

    @GetMapping({"/withdrawals/balance", "/withdrawal-balance"})
    @Operation(summary = "Get withdrawable partner balance")
    public ApiResponse<Map<String, Object>> withdrawalBalance(@RequestAttribute("userId") Long partnerId) {
        return ApiResponse.success("Withdrawable balance fetched",
                Map.of("currency", "INR", "withdrawableAmount", partnerSettlementService.withdrawableBalance(partnerId)));
    }

    @GetMapping("/inbox/activity")
    @Operation(summary = "Get unified partner inbox and activity feed")
    public ApiResponse<PageResponseDto<Map<String, Object>>> activityFeed(@RequestAttribute("userId") Long partnerId,
                                                                          @RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success("Partner activity feed fetched",
                partnerActivityFeedService.activity(partnerId, page, size));
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
