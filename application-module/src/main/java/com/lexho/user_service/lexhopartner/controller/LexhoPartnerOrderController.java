package com.lexho.user_service.lexhopartner.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.lexhopartner.dto.DashboardStatsDto;
import com.lexho.user_service.lexhopartner.dto.OrderResponseDto;
import com.lexho.user_service.lexhopartner.dto.OrderTimelineDto;
import com.lexho.user_service.lexhopartner.dto.PageResponseDto;
import com.lexho.user_service.lexhopartner.service.PartnerOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping({"/api/orders", "/api/v1/orders"})
@PreAuthorize("hasAnyRole('PARTNER','ADMIN')")
@Tag(name = "Lexho Partner Orders")
public class LexhoPartnerOrderController {

    private final PartnerOrderService partnerOrderService;

    public LexhoPartnerOrderController(PartnerOrderService partnerOrderService) {
        this.partnerOrderService = partnerOrderService;
    }

    @GetMapping("/new")
    @Operation(summary = "Get new/nearby orders")
    public ApiResponse<PageResponseDto<OrderResponseDto>> newOrders(@RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "20") int size,
                                                                    @RequestParam(defaultValue = "createdAt") String sortBy,
                                                                    @RequestParam(defaultValue = "desc") String direction) {
        return ApiResponse.success("Orders fetched", partnerOrderService.getNewOrders(page, size, sortBy, direction));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order details")
    public ApiResponse<OrderResponseDto> getOrder(@RequestAttribute("userId") Long partnerId, @PathVariable Long id) {
        return ApiResponse.success("Order fetched", partnerOrderService.getOrder(partnerId, id));
    }

    @GetMapping("/{id}/timeline")
    @Operation(summary = "Get order lifecycle timeline")
    public ApiResponse<List<OrderTimelineDto>> timeline(@RequestAttribute("userId") Long partnerId, @PathVariable Long id) {
        return ApiResponse.success("Order timeline fetched", partnerOrderService.timeline(partnerId, id));
    }

    @PostMapping("/{id}/accept")
    public ApiResponse<OrderResponseDto> accept(@RequestAttribute("userId") Long partnerId, @PathVariable Long id) {
        return ApiResponse.success("Order accepted", partnerOrderService.acceptOrder(partnerId, id));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<OrderResponseDto> reject(@RequestAttribute("userId") Long partnerId, @PathVariable Long id) {
        return ApiResponse.success("Order rejected", partnerOrderService.rejectOrder(partnerId, id));
    }

    @PostMapping("/{id}/start")
    public ApiResponse<OrderResponseDto> start(@RequestAttribute("userId") Long partnerId,
                                               @PathVariable Long id,
                                               @RequestPart(value = "beforeImage", required = false) MultipartFile beforeImage) {
        return ApiResponse.success("Order started", partnerOrderService.startOrder(partnerId, id, beforeImage));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<OrderResponseDto> complete(@RequestAttribute("userId") Long partnerId,
                                                  @PathVariable Long id,
                                                  @RequestPart(value = "afterImage", required = false) MultipartFile afterImage) {
        return ApiResponse.success("Order completed", partnerOrderService.completeOrder(partnerId, id, afterImage));
    }

    @GetMapping("/history")
    public ApiResponse<PageResponseDto<OrderResponseDto>> history(@RequestAttribute("userId") Long partnerId,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "20") int size,
                                                                  @RequestParam(defaultValue = "createdAt") String sortBy,
                                                                  @RequestParam(defaultValue = "desc") String direction) {
        return ApiResponse.success("Order history fetched",
                partnerOrderService.getHistory(partnerId, page, size, sortBy, direction));
    }

    @GetMapping("/dashboard")
    public ApiResponse<DashboardStatsDto> dashboard(@RequestAttribute("userId") Long partnerId) {
        return ApiResponse.success("Dashboard fetched", partnerOrderService.dashboard(partnerId));
    }
}
