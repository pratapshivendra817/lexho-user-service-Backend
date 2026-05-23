package com.lexho.user_service.dispatch.controller;

import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.user_service.dispatch.dto.DispatchRequest;
import com.lexho.user_service.dispatch.dto.OrderLifecycleUpdateRequest;
import com.lexho.user_service.dispatch.service.DispatchOrchestrationService;
import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.service.ApiRateLimiterService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping({"/api/admin/dispatch", "/api/v1/admin/dispatch"})
@PreAuthorize("hasRole('ADMIN')")
public class AdminDispatchController {

    private final DispatchOrchestrationService dispatchOrchestrationService;
    private final OrderRepository orderRepository;
    private final ApiRateLimiterService apiRateLimiterService;

    public AdminDispatchController(DispatchOrchestrationService dispatchOrchestrationService,
                                   OrderRepository orderRepository,
                                   ApiRateLimiterService apiRateLimiterService) {
        this.dispatchOrchestrationService = dispatchOrchestrationService;
        this.orderRepository = orderRepository;
        this.apiRateLimiterService = apiRateLimiterService;
    }

    @PostMapping("/orders/{orderId}")
    public ApiResponse<Map<String, Object>> dispatchOrder(@PathVariable Long orderId,
                                                           @Valid @RequestBody DispatchRequest request) {
        apiRateLimiterService.check("admin_dispatch", String.valueOf(orderId), 20, Duration.ofMinutes(1),
                "Dispatch rate limit exceeded");
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        int created = dispatchOrchestrationService.dispatchOrder(
                orderId,
                order.getUserId(),
                request.addressId,
                request.serviceId,
                request.quantity,
                order.getScheduledAt()
        );
        return ApiResponse.success("Dispatch processed", Map.of(
                "orderId", orderId,
                "requestsCreated", created
        ));
    }

    @PatchMapping("/orders/{orderId}/lifecycle")
    public ApiResponse<Map<String, Object>> updateLifecycle(@PathVariable Long orderId,
                                                             @Valid @RequestBody OrderLifecycleUpdateRequest request) {
        apiRateLimiterService.check("admin_lifecycle", String.valueOf(orderId), 60, Duration.ofMinutes(1),
                "Too many lifecycle updates");
        return ApiResponse.success("Order lifecycle updated", Map.of(
                "orderId", orderId,
                "status", dispatchOrchestrationService.transitionOrderStatus(orderId, request.status).name()
        ));
    }
}
