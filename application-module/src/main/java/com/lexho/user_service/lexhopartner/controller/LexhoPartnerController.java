package com.lexho.user_service.lexhopartner.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.lexhopartner.dto.PartnerDeviceTokenRequest;
import com.lexho.user_service.lexhopartner.dto.PartnerAvailabilityResponse;
import com.lexho.user_service.lexhopartner.dto.PartnerBannerPageResponse;
import com.lexho.user_service.lexhopartner.dto.PartnerEarningTransactionDto;
import com.lexho.user_service.lexhopartner.dto.PartnerEarningsSummaryDto;
import com.lexho.user_service.lexhopartner.dto.PartnerLeaveRequest;
import com.lexho.user_service.lexhopartner.dto.PartnerOnboardingRequest;
import com.lexho.user_service.lexhopartner.dto.PartnerOnboardingResponse;
import com.lexho.user_service.lexhopartner.dto.PageResponseDto;
import com.lexho.user_service.lexhopartner.dto.PartnerServiceAreaRequest;
import com.lexho.user_service.lexhopartner.dto.PartnerCatalogCategoryResponse;
import com.lexho.user_service.lexhopartner.dto.UpdateAvailabilityRequest;
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
import com.lexho.user_service.lexhopartner.service.PartnerOperationalProfileService;
import com.lexho.user_service.lexhopartner.service.PartnerOrderService;
import com.lexho.user_service.lexhopartner.service.PartnerOperationsService;
import com.lexho.user_service.lexhopartner.service.PartnerSettlementService;
import com.lexho.user_service.lexhopartner.service.PartnerCatalogService;
import com.lexho.user_service.lexhopartner.util.FileStorageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.List;
import java.util.Map;
import java.time.LocalDate;

@RestController
@RequestMapping({"/api/partner", "/api/v1/partner"})
@PreAuthorize("hasRole('PARTNER')")
@Tag(name = "Lexho Partner")
public class LexhoPartnerController {
    private static final String ATTR_USER_ID = "userId";

    private final PartnerOperationalProfileService partnerProfileService;
    private final PartnerOrderService partnerOrderService;
    private final PartnerBannerService partnerBannerService;
    private final PartnerSettlementService partnerSettlementService;
    private final PartnerDeviceTokenService partnerDeviceTokenService;
    private final PartnerOperationsService partnerOperationsService;
    private final PartnerActivityFeedService partnerActivityFeedService;
    private final PartnerCatalogService partnerCatalogService;
    private final FileStorageUtil fileStorageUtil;

    public LexhoPartnerController(PartnerOperationalProfileService partnerProfileService,
                                  PartnerOrderService partnerOrderService,
                                  PartnerBannerService partnerBannerService,
                                  PartnerSettlementService partnerSettlementService,
                                  PartnerDeviceTokenService partnerDeviceTokenService,
                                  PartnerOperationsService partnerOperationsService,
                                  PartnerActivityFeedService partnerActivityFeedService,
                                  PartnerCatalogService partnerCatalogService,
                                  FileStorageUtil fileStorageUtil) {
        this.partnerProfileService = partnerProfileService;
        this.partnerOrderService = partnerOrderService;
        this.partnerBannerService = partnerBannerService;
        this.partnerSettlementService = partnerSettlementService;
        this.partnerDeviceTokenService = partnerDeviceTokenService;
        this.partnerOperationsService = partnerOperationsService;
        this.partnerActivityFeedService = partnerActivityFeedService;
        this.partnerCatalogService = partnerCatalogService;
        this.fileStorageUtil = fileStorageUtil;
    }

    @GetMapping({"/categories", "/service-types"})
    @Operation(summary = "Get active service type categories for partners")
    public ApiResponse<Map<String, List<PartnerCatalogCategoryResponse>>> serviceTypes(
            @RequestAttribute(ATTR_USER_ID) Long partnerId,
            @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.success("Service types fetched", partnerCatalogService.categoryDropdown(partnerId, limit));
    }

    @GetMapping({"/onboarding", "/profile"})
    @Operation(summary = "Get partner onboarding details")
    public ApiResponse<PartnerOnboardingResponse> onboarding(
            @RequestAttribute(ATTR_USER_ID) Long partnerId,
            @RequestAttribute(value = "mobile", required = false) String mobileNumber) {
        return ApiResponse.success("Partner onboarding fetched",
                withLoginMobile(partnerProfileService.onboardingDetails(partnerId), mobileNumber));
    }

    @RequestMapping(value = "/onboarding", method = {RequestMethod.PUT, RequestMethod.POST},
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Submit or update partner onboarding details with uploaded documents")
    public ApiResponse<PartnerOnboardingResponse> submitOnboardingMultipart(
            @RequestAttribute(ATTR_USER_ID) Long partnerId,
            @RequestAttribute(value = "mobile", required = false) String mobileNumber,
            HttpServletRequest servletRequest) {
        MultipartHttpServletRequest multipartRequest = requireMultipart(servletRequest);
        List<String> normalizedSkills = normalizeMultipartSkills(paramValues(
                multipartRequest, "skills", "skill", "serviceType", "serviceTypes"));
        if (normalizedSkills.isEmpty()) {
            throw new BadRequestException("skills is required");
        }
        PartnerOnboardingRequest onboardingRequest = new PartnerOnboardingRequest(
                requiredParam(multipartRequest, "fullName", "full_name", "name"),
                trimToNull(mobileNumber),
                parseRequiredDate(requiredParam(multipartRequest, "dateOfBirth", "date_of_birth", "dob")),
                requiredParam(multipartRequest, "gender"),
                requiredParam(multipartRequest, "addressLine1", "address", "address_line1", "fullAddress"),
                requiredParam(multipartRequest, "city"),
                requiredParam(multipartRequest, "state"),
                requiredParam(multipartRequest, "pincode", "pinCode", "pin_code"),
                parseOptionalLong(optionalParam(multipartRequest, "serviceCategoryId", "service_category_id", "categoryId")),
                normalizedSkills,
                requiredParam(multipartRequest, "accountNumber", "account_number", "bankAccountNumber"),
                requiredParam(multipartRequest, "ifscCode", "ifsc", "ifsc_code"),
                storedRequiredDocument(
                        firstFile(multipartRequest, "aadhaarFront", "aadhaar", "aadhar", "aadhaar_front", "aadhaarImage"),
                        "aadhaar-front"),
                storedRequiredDocument(
                        firstFile(multipartRequest, "aadhaarBack", "aadhaarBackImage", "aadhaar_back",
                                "aadharBack", "aadharBackImage", "aadhar_back"),
                        "aadhaar-back"),
                storedRequiredDocument(
                        firstFile(multipartRequest, "panCard", "panCardImage", "pan", "pan_card", "panImage"),
                        "pan-card"),
                storedRequiredImage(
                        firstFile(multipartRequest, "selfie", "selfieImage", "selfiePhoto", "selfie_photo"),
                        "selfie"),
                storedDocument(
                        firstFile(multipartRequest, "certificate", "certificateImage", "certificateFile"),
                        "certificate"));
        return ApiResponse.success("Partner onboarding submitted",
                withLoginMobile(partnerProfileService.submitOnboarding(partnerId, onboardingRequest), mobileNumber));
    }

    @PostMapping("/device-token")
    @Operation(summary = "Register partner FCM device token")
    public ApiResponse<Map<String, Object>> registerDeviceToken(@RequestAttribute(ATTR_USER_ID) Long partnerId,
                                                                @Valid @RequestBody PartnerDeviceTokenRequest request) {
        return ApiResponse.success("Device token registered", partnerDeviceTokenService.register(partnerId, request));
    }

    @DeleteMapping("/device-token")
    @Operation(summary = "Deactivate partner FCM device token")
    public ApiResponse<Map<String, Object>> deleteDeviceToken(@RequestAttribute(ATTR_USER_ID) Long partnerId,
                                                              @Valid @RequestBody PartnerDeviceTokenRequest request) {
        return ApiResponse.success("Device token deleted", partnerDeviceTokenService.delete(partnerId, request));
    }

    @PutMapping("/availability")
    @Operation(summary = "Update partner availability")
    public PartnerAvailabilityResponse availability(@RequestAttribute(ATTR_USER_ID) Long partnerId,
                                                    @Valid @RequestBody UpdateAvailabilityRequest request) {
        return partnerProfileService.updateAvailability(partnerId, request);
    }

    @GetMapping("/dashboard/stats")
    @Operation(summary = "Get partner dashboard stats")
    public DashboardStatsDto dashboardStats(@RequestAttribute(ATTR_USER_ID) Long partnerId) {
        return partnerOrderService.dashboard(partnerId);
    }

    @GetMapping("/banners")
    @Operation(summary = "Get partner home banners")
    public PartnerBannerPageResponse banners(@RequestAttribute(ATTR_USER_ID) Long partnerId,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "5") int size) {
        return partnerBannerService.partnerBanners(partnerId, page, size);
    }

    @GetMapping("/earnings/summary")
    @Operation(summary = "Get partner earnings summary")
    public ApiResponse<PartnerEarningsSummaryDto> earningsSummary(@RequestAttribute(ATTR_USER_ID) Long partnerId) {
        return ApiResponse.success("Earnings summary fetched", partnerOrderService.earningsSummary(partnerId));
    }

    @GetMapping("/earnings/transactions")
    @Operation(summary = "Get partner earning transactions")
    public ApiResponse<PageResponseDto<PartnerEarningTransactionDto>> earningTransactions(
            @RequestAttribute(ATTR_USER_ID) Long partnerId,
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
    public ApiResponse<PageResponseDto<PartnerPayoutDto>> payouts(@RequestAttribute(ATTR_USER_ID) Long partnerId,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success("Payouts fetched", partnerSettlementService.payouts(partnerId, page, size));
    }

    @GetMapping("/performance")
    @Operation(summary = "Get partner performance metrics")
    public ApiResponse<Map<String, Object>> performance(@RequestAttribute(ATTR_USER_ID) Long partnerId) {
        return ApiResponse.success("Performance fetched", partnerOperationsService.performance(partnerId));
    }

    @GetMapping("/ratings")
    @Operation(summary = "Get partner ratings and reviews")
    public ApiResponse<Map<String, Object>> ratings(@RequestAttribute(ATTR_USER_ID) Long partnerId,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success("Ratings fetched", partnerOperationsService.ratings(partnerId, page, size));
    }

    @PostMapping("/leave")
    @Operation(summary = "Create partner leave")
    public ApiResponse<Map<String, Object>> createLeave(@RequestAttribute(ATTR_USER_ID) Long partnerId,
                                                        @Valid @RequestBody PartnerLeaveRequest request) {
        return ApiResponse.success("Leave created", partnerOperationsService.createLeave(partnerId, request));
    }

    @PatchMapping("/leave/{id}/cancel")
    @Operation(summary = "Cancel partner leave")
    public ApiResponse<Map<String, Object>> cancelLeave(@RequestAttribute(ATTR_USER_ID) Long partnerId,
                                                        @PathVariable Long id) {
        return ApiResponse.success("Leave cancelled", partnerOperationsService.cancelLeave(partnerId, id));
    }

    @GetMapping("/leave")
    @Operation(summary = "Get partner leave history")
    public ApiResponse<List<Map<String, Object>>> leaves(@RequestAttribute(ATTR_USER_ID) Long partnerId) {
        return ApiResponse.success("Leaves fetched", partnerOperationsService.leaves(partnerId));
    }

    @GetMapping("/service-areas")
    @Operation(summary = "Get partner service area preferences")
    public ApiResponse<List<Map<String, Object>>> serviceAreas(@RequestAttribute(ATTR_USER_ID) Long partnerId) {
        return ApiResponse.success("Service areas fetched", partnerOperationsService.serviceAreas(partnerId));
    }

    @PutMapping("/service-areas")
    @Operation(summary = "Update partner service area preferences")
    public ApiResponse<List<Map<String, Object>>> updateServiceAreas(@RequestAttribute(ATTR_USER_ID) Long partnerId,
                                                                     @Valid @RequestBody PartnerServiceAreaRequest request) {
        return ApiResponse.success("Service areas updated", partnerOperationsService.updateServiceAreas(partnerId, request));
    }

    @PatchMapping("/service-areas")
    @Operation(summary = "Update partner service area preferences using selected zones")
    public ApiResponse<List<Map<String, Object>>> patchServiceAreas(@RequestAttribute(ATTR_USER_ID) Long partnerId,
                                                                    @RequestBody List<PartnerServiceAreaRequest.ServiceAreaItem> areas) {
        return ApiResponse.success("Service areas updated", partnerOperationsService.updateServiceAreas(partnerId, areas));
    }

    @GetMapping("/warnings")
    @Operation(summary = "Get partner warnings")
    public ApiResponse<List<Map<String, Object>>> warnings(@RequestAttribute(ATTR_USER_ID) Long partnerId) {
        return ApiResponse.success("Warnings fetched", partnerOperationsService.warnings(partnerId));
    }

    @GetMapping("/warnings/{warningId}")
    @Operation(summary = "Get partner warning details")
    public ApiResponse<Map<String, Object>> warning(@RequestAttribute(ATTR_USER_ID) Long partnerId,
                                                    @PathVariable Long warningId) {
        return ApiResponse.success("Warning fetched", partnerOperationsService.warningDetail(partnerId, warningId));
    }

    @PatchMapping("/warnings/{warningId}/acknowledge")
    @Operation(summary = "Acknowledge partner warning")
    public ApiResponse<Map<String, Object>> acknowledgeWarning(@RequestAttribute(ATTR_USER_ID) Long partnerId,
                                                               @PathVariable Long warningId) {
        return ApiResponse.success("Warning acknowledged",
                partnerOperationsService.acknowledgeWarning(partnerId, warningId));
    }

    @PostMapping("/warnings/{warningId}/dispute")
    @Operation(summary = "Dispute partner warning")
    public ApiResponse<Map<String, Object>> disputeWarning(@RequestAttribute(ATTR_USER_ID) Long partnerId,
                                                           @PathVariable Long warningId,
                                                           @Valid @RequestBody PartnerWarningDisputeRequest request) {
        return ApiResponse.success("Warning dispute submitted",
                partnerOperationsService.disputeWarning(partnerId, warningId, request.reason(), request.attachmentUrls()));
    }

    @PostMapping("/warnings/{warningId}/comments")
    @Operation(summary = "Add warning dispute comment")
    public ApiResponse<Map<String, Object>> warningComment(@RequestAttribute(ATTR_USER_ID) Long partnerId,
                                                           @PathVariable Long warningId,
                                                           @Valid @RequestBody PartnerWarningCommentRequest request) {
        return ApiResponse.success("Warning comment added",
                partnerOperationsService.addWarningComment(partnerId, warningId, request));
    }

    @GetMapping("/warnings/{warningId}/stream")
    @Operation(summary = "Stream warning updates")
    public SseEmitter warningStream(@RequestAttribute(ATTR_USER_ID) Long partnerId,
                                    @PathVariable Long warningId) {
        return partnerOperationsService.warningStream(partnerId, warningId);
    }

    @GetMapping("/payouts/{payoutId}")
    @Operation(summary = "Get partner payout details")
    public ApiResponse<PartnerPayoutDto> payout(@RequestAttribute(ATTR_USER_ID) Long partnerId,
                                                @PathVariable Long payoutId) {
        return ApiResponse.success("Payout fetched", partnerSettlementService.payout(partnerId, payoutId));
    }

    @GetMapping("/bank-details")
    @Operation(summary = "Get masked partner bank details")
    public ApiResponse<PartnerBankDetailsDto> bankDetails(@RequestAttribute(ATTR_USER_ID) Long partnerId) {
        return ApiResponse.success("Bank details fetched", partnerProfileService.bankDetails(partnerId));
    }

    @PutMapping("/bank-details")
    @Operation(summary = "Update partner bank details")
    public ApiResponse<PartnerBankDetailsDto> updateBankDetails(@RequestAttribute(ATTR_USER_ID) Long partnerId,
                                                                @Valid @RequestBody UpdatePartnerBankDetailsRequest request) {
        return ApiResponse.success("Bank details updated", partnerProfileService.updateBankDetails(partnerId, request));
    }

    @PostMapping("/withdrawals")
    @Operation(summary = "Request withdrawal")
    public ApiResponse<PartnerWithdrawalDto> requestWithdrawal(@RequestAttribute(ATTR_USER_ID) Long partnerId,
                                                                @Valid @RequestBody CreateWithdrawalRequest request) {
        return ApiResponse.success("Withdrawal requested",
                partnerSettlementService.requestWithdrawal(partnerId, request));
    }

    @GetMapping("/withdrawals")
    @Operation(summary = "Get partner withdrawal requests")
    public ApiResponse<PageResponseDto<PartnerWithdrawalDto>> withdrawals(@RequestAttribute(ATTR_USER_ID) Long partnerId,
                                                                          @RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success("Withdrawals fetched", partnerSettlementService.withdrawals(partnerId, page, size));
    }

    @GetMapping({"/withdrawals/balance", "/withdrawal-balance"})
    @Operation(summary = "Get withdrawable partner balance")
    public ApiResponse<Map<String, Object>> withdrawalBalance(@RequestAttribute(ATTR_USER_ID) Long partnerId) {
        return ApiResponse.success("Withdrawable balance fetched",
                Map.of("currency", "INR", "availableBalance", partnerSettlementService.withdrawableBalance(partnerId)));
    }

    @GetMapping("/inbox/activity")
    @Operation(summary = "Get unified partner inbox and activity feed")
    public ApiResponse<PageResponseDto<Map<String, Object>>> activityFeed(@RequestAttribute(ATTR_USER_ID) Long partnerId,
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

    private MultipartHttpServletRequest requireMultipart(HttpServletRequest request) {
        if (request instanceof MultipartHttpServletRequest multipartRequest) {
            return multipartRequest;
        }
        throw new BadRequestException("Invalid multipart request");
    }

    private String requiredParam(HttpServletRequest request, String... names) {
        String value = optionalParam(request, names);
        if (value == null) {
            throw new BadRequestException(names[0] + " is required");
        }
        return value;
    }

    private String optionalParam(HttpServletRequest request, String... names) {
        for (String name : names) {
            String value = request.getParameter(name);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private List<String> paramValues(HttpServletRequest request, String... names) {
        java.util.ArrayList<String> values = new java.util.ArrayList<>();
        for (String name : names) {
            String[] current = request.getParameterValues(name);
            if (current != null) {
                values.addAll(java.util.Arrays.asList(current));
            }
        }
        return values;
    }

    private MultipartFile firstFile(MultipartHttpServletRequest request, String... names) {
        for (String name : names) {
            MultipartFile file = request.getFile(name);
            if (hasFile(file)) {
                return file;
            }
        }
        return null;
    }

    private LocalDate parseRequiredDate(String value) {
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception ex) {
            throw new BadRequestException("dateOfBirth must use YYYY-MM-DD");
        }
    }

    private Long parseRequiredLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (Exception ex) {
            throw new BadRequestException("serviceCategoryId is invalid");
        }
    }

    private Long parseOptionalLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseRequiredLong(value);
    }

    private String storedImage(MultipartFile file, String documentName) {
        if (hasFile(file)) {
            return fileStorageUtil.storeImage(file, "partner-onboarding/" + documentName);
        }
        return null;
    }

    private String storedRequiredImage(MultipartFile file, String documentName) {
        String stored = storedImage(file, documentName);
        if (stored == null) {
            throw new BadRequestException(documentName + " is required");
        }
        return stored;
    }

    private String storedDocument(MultipartFile file, String documentName) {
        if (hasFile(file)) {
            return fileStorageUtil.storeDocument(file, "partner-onboarding/" + documentName);
        }
        return null;
    }

    private String storedRequiredDocument(MultipartFile file, String documentName) {
        String stored = storedDocument(file, documentName);
        if (stored == null) {
            throw new BadRequestException(documentName + " is required");
        }
        return stored;
    }

    private boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private List<String> normalizeMultipartSkills(List<String> skills) {
        if (skills == null) {
            return List.of();
        }
        return skills.stream()
                .flatMap(value -> java.util.Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private PartnerOnboardingResponse withLoginMobile(PartnerOnboardingResponse response, String mobileNumber) {
        String normalizedMobile = trimToNull(mobileNumber);
        if (normalizedMobile == null || response == null) {
            return response;
        }
        return new PartnerOnboardingResponse(
                response.partnerId(),
                response.fullName(),
                normalizedMobile,
                response.dateOfBirth(),
                response.gender(),
                response.addressLine1(),
                response.city(),
                response.state(),
                response.pincode(),
                response.serviceCategoryId(),
                response.skills(),
                response.maskedAccountNumber(),
                response.accountNumber(),
                response.ifscCode(),
                response.bankVerificationStatus(),
                response.aadhaarFrontUrl(),
                response.aadhaarBackUrl(),
                response.panCardUrl(),
                response.selfieUrl(),
                response.certificateUrl(),
                response.applicationStatus(),
                response.documentStatus(),
                response.rejectionReason(),
                response.reviewedByAdminId(),
                response.reviewedAt(),
                response.submittedAt(),
                response.documents(),
                response.documentMap());
    }
}
