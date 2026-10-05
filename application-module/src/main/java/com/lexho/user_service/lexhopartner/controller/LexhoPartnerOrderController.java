package com.lexho.user_service.lexhopartner.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.lexhopartner.dto.DashboardStatsDto;
import com.lexho.user_service.lexhopartner.dto.OrderResponseDto;
import com.lexho.user_service.lexhopartner.dto.OrderTimelineDto;
import com.lexho.user_service.lexhopartner.dto.PageResponseDto;
import com.lexho.user_service.lexhopartner.dto.ServicePinVerifyRequest;
import com.lexho.user_service.lexhopartner.service.PartnerOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.lexho.user_service.lexhopartner.controller.PartnerControllerText.ATTR_USER_ID;

@RestController
@RequestMapping({"/api/orders", "/api/v1/orders"})
@PreAuthorize("hasAnyRole('PARTNER','ADMIN')")
@Tag(name = "Lexho Partner Orders")
public class LexhoPartnerOrderController {

    private static final String DEFAULT_SORT_CREATED_AT = "createdAt";

    private final PartnerOrderService partnerOrderService;

    public LexhoPartnerOrderController(PartnerOrderService partnerOrderService) {
        this.partnerOrderService = partnerOrderService;
    }

    @GetMapping("/new")
    @Operation(summary = "Get new/nearby orders")
    public ApiResponse<PageResponseDto<OrderResponseDto>> newOrders(@RequestAttribute(ATTR_USER_ID) Long partnerId,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "20") int size,
                                                                    @RequestParam(defaultValue = DEFAULT_SORT_CREATED_AT) String sortBy,
                                                                    @RequestParam(defaultValue = "desc") String direction) {
        return ApiResponse.success(PartnerControllerText.MSG_ORDERS_FETCHED,
                partnerOrderService.getNewOrders(partnerId, page, size, sortBy, direction));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order details")
    public ApiResponse<OrderResponseDto> getOrder(@RequestAttribute(ATTR_USER_ID) Long partnerId, @PathVariable Long id) {
        return ApiResponse.success(PartnerControllerText.MSG_ORDER_FETCHED, partnerOrderService.getOrder(partnerId, id));
    }

    @GetMapping("/{id}/timeline")
    @Operation(summary = "Get order lifecycle timeline")
    public ApiResponse<List<OrderTimelineDto>> timeline(@RequestAttribute(ATTR_USER_ID) Long partnerId, @PathVariable Long id) {
        return ApiResponse.success(PartnerControllerText.MSG_ORDER_TIMELINE_FETCHED,
                partnerOrderService.timeline(partnerId, id));
    }

    @PostMapping("/{id}/accept")
    public ApiResponse<OrderResponseDto> accept(@RequestAttribute(ATTR_USER_ID) Long partnerId, @PathVariable Long id) {
        return ApiResponse.success(PartnerControllerText.MSG_ORDER_ACCEPTED, partnerOrderService.acceptOrder(partnerId, id));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<OrderResponseDto> reject(@RequestAttribute(ATTR_USER_ID) Long partnerId, @PathVariable Long id) {
        return ApiResponse.success(PartnerControllerText.MSG_ORDER_REJECTED, partnerOrderService.rejectOrder(partnerId, id));
    }

    @PostMapping("/{id}/arrive")
    public ApiResponse<OrderResponseDto> arrive(@RequestAttribute(ATTR_USER_ID) Long partnerId, @PathVariable Long id) {
        return ApiResponse.success(PartnerControllerText.MSG_PARTNER_ARRIVED, partnerOrderService.arriveOrder(partnerId, id));
    }

    @PostMapping("/{id}/start")
    public ApiResponse<OrderResponseDto> start(@RequestAttribute(ATTR_USER_ID) Long partnerId,
                                               @PathVariable Long id,
                                               @RequestPart(value = "beforeImage", required = false) MultipartFile beforeImage,
                                               @RequestParam(value = "pin", required = false) String pin) {
        return ApiResponse.success(PartnerControllerText.MSG_ORDER_STARTED,
                partnerOrderService.startOrder(partnerId, id, beforeImage, pin));
    }

    @PostMapping("/{id}/start/verify")
    public ApiResponse<OrderResponseDto> verifyStartPin(@RequestAttribute(ATTR_USER_ID) Long partnerId,
                                                        @PathVariable Long id,
                                                        @Valid @RequestBody ServicePinVerifyRequest request) {
        return ApiResponse.success(PartnerControllerText.MSG_ORDER_STARTED,
                partnerOrderService.startOrder(partnerId, id, null, request.pin()));
    }

    @PostMapping("/{id}/complete-pin")
    public ApiResponse<OrderResponseDto> requestCompletePin(@RequestAttribute(ATTR_USER_ID) Long partnerId,
                                                            @PathVariable Long id) {
        return ApiResponse.success(PartnerControllerText.MSG_COMPLETE_PIN_REQUESTED,
                partnerOrderService.requestCompletePin(partnerId, id));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<OrderResponseDto> complete(@RequestAttribute(ATTR_USER_ID) Long partnerId,
                                                  @PathVariable Long id,
                                                  @RequestPart(value = "afterImage", required = false) MultipartFile afterImage,
                                                  @RequestParam(value = "pin", required = false) String pin) {
        return ApiResponse.success(PartnerControllerText.MSG_ORDER_COMPLETED,
                partnerOrderService.completeOrder(partnerId, id, afterImage, pin));
    }

    @PostMapping("/{id}/complete/verify")
    public ApiResponse<OrderResponseDto> verifyCompletePin(@RequestAttribute(ATTR_USER_ID) Long partnerId,
                                                           @PathVariable Long id,
                                                           @Valid @RequestBody ServicePinVerifyRequest request) {
        return ApiResponse.success(PartnerControllerText.MSG_ORDER_COMPLETED,
                partnerOrderService.completeOrder(partnerId, id, null, request.pin()));
    }

    @GetMapping("/history")
    public ApiResponse<PageResponseDto<OrderResponseDto>> history(@RequestAttribute(ATTR_USER_ID) Long partnerId,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "20") int size,
                                                            @RequestParam(defaultValue = DEFAULT_SORT_CREATED_AT) String sortBy,
                                                                  @RequestParam(defaultValue = "desc") String direction) {
        return ApiResponse.success(PartnerControllerText.MSG_ORDER_HISTORY_FETCHED,
                partnerOrderService.getHistory(partnerId, page, size, sortBy, direction));
    }

    @GetMapping("/dashboard")
    public ApiResponse<DashboardStatsDto> dashboard(@RequestAttribute(ATTR_USER_ID) Long partnerId) {
        return ApiResponse.success(PartnerControllerText.MSG_DASHBOARD_FETCHED, partnerOrderService.dashboard(partnerId));
    }
}
