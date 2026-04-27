package com.lexho.user_service.controller;

import com.lexho.common.enums.OrderStatus;
import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.dto.OrderResponseDTO;
import com.lexho.user_service.dto.PlaceOrderRequest;
import com.lexho.user_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/{id}")
    public ApiResponse<OrderResponseDTO> getOrder(@RequestAttribute("userId") Long userId,
                                                  @PathVariable Long id) {
        return ApiResponse.success("Order fetched", orderService.getOrder(id, userId));
    }

    @GetMapping
    public ApiResponse<List<OrderResponseDTO>> getUserOrders(@RequestAttribute("userId") Long userId) {
        return ApiResponse.success("Orders fetched", orderService.getUserOrders(userId));
    }

    @PostMapping("/place")
    public ApiResponse<OrderResponseDTO> placeOrder(@RequestAttribute("userId") Long userId,
                                                    @RequestBody PlaceOrderRequest request) {
        return ApiResponse.success("Order placed", orderService.placeOrder(userId, request));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<OrderResponseDTO> updateStatus(@PathVariable Long id,
                                                      @RequestParam OrderStatus status) {
        return ApiResponse.success("Status updated", orderService.updateStatus(id, status));
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<OrderResponseDTO> cancelOrder(@RequestAttribute("userId") Long userId,
                                                     @PathVariable Long id) {
        return ApiResponse.success("Order cancelled", orderService.cancelOrder(id, userId));
    }
}

