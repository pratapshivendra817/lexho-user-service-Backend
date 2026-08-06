package com.lexho.user_service.partnerrequest.controller;

import com.lexho.user_service.config.SecurityExposureProperties;
import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.partnerrequest.dto.PagedResponse;
import com.lexho.user_service.partnerrequest.dto.PartnerIncomingRequestResponse;
import com.lexho.user_service.partnerrequest.dto.PartnerRejectRequest;
import com.lexho.user_service.partnerrequest.dto.PartnerRequestActionResponse;
import com.lexho.user_service.partnerrequest.dto.PartnerStatusResponse;
import com.lexho.user_service.partnerrequest.dto.PartnerStatusUpdateRequest;
import com.lexho.user_service.partnerrequest.model.PartnerServiceRequestStatus;
import com.lexho.user_service.partnerrequest.service.PartnerRequestService;
import com.lexho.user_service.service.ApiRateLimiterService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping({"/api/partner", "/api/v1/partner"})
public class PartnerRequestController {

    private final PartnerRequestService partnerRequestService;
    private final ApiRateLimiterService apiRateLimiterService;
    private final SecurityExposureProperties securityExposureProperties;

    public PartnerRequestController(PartnerRequestService partnerRequestService,
                                    ApiRateLimiterService apiRateLimiterService,
                                    SecurityExposureProperties securityExposureProperties) {
        this.partnerRequestService = partnerRequestService;
        this.apiRateLimiterService = apiRateLimiterService;
        this.securityExposureProperties = securityExposureProperties;
    }

    @PatchMapping("/me/status")
    public ApiResponse<PartnerStatusResponse> updateStatus(@RequestAttribute("userId") Long partnerId,
                                                           @Valid @RequestBody PartnerStatusUpdateRequest request) {
        return ApiResponse.success("Partner status updated", partnerRequestService.updateOnlineStatus(partnerId, request));
    }

    @GetMapping("/requests")
    public ApiResponse<PagedResponse<PartnerIncomingRequestResponse>> requests(
            @RequestAttribute("userId") Long partnerId,
            @RequestParam(required = false) PartnerServiceRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success("Requests fetched",
                partnerRequestService.getIncomingRequests(partnerId, status, page, size));
    }

    @GetMapping("/requests/{requestId}")
    public ApiResponse<PartnerIncomingRequestResponse> detail(@RequestAttribute("userId") Long partnerId,
                                                              @PathVariable Long requestId) {
        return ApiResponse.success("Request detail fetched", partnerRequestService.getRequestDetail(partnerId, requestId));
    }

    @PatchMapping("/requests/{requestId}/accept")
    public ApiResponse<PartnerRequestActionResponse> accept(@RequestAttribute("userId") Long partnerId,
                                                            @PathVariable Long requestId) {
        apiRateLimiterService.check("partner_accept", String.valueOf(partnerId), 20, Duration.ofMinutes(1),
                "Too many accept attempts. Please slow down");
        return ApiResponse.success("Request accepted", partnerRequestService.accept(partnerId, requestId));
    }

    @PatchMapping("/requests/{requestId}/reject")
    public ApiResponse<PartnerRequestActionResponse> reject(@RequestAttribute("userId") Long partnerId,
                                                            @PathVariable Long requestId,
                                                            @Valid @RequestBody(required = false) PartnerRejectRequest request) {
        apiRateLimiterService.check("partner_reject", String.valueOf(partnerId), 20, Duration.ofMinutes(1),
                "Too many reject attempts. Please slow down");
        return ApiResponse.success("Request rejected", partnerRequestService.reject(partnerId, requestId, request));
    }

    @PatchMapping("/requests/dev/seed")
    public ApiResponse<PartnerIncomingRequestResponse> seedPendingForDev(@RequestAttribute("userId") Long partnerId) {
        if (!securityExposureProperties.isAllowPartnerRequestDevSeed()) {
            throw new ResourceNotFoundException("Endpoint not found");
        }
        return ApiResponse.success("Dev pending request created", partnerRequestService.createDevPendingRequest(partnerId));
    }
}
