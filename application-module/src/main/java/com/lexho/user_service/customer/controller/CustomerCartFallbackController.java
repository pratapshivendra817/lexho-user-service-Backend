package com.lexho.user_service.customer.controller;

import com.lexho.user_service.cartconfig.dto.CartConfigResponse;
import com.lexho.user_service.customer.dto.CustomerAddToCartRequest;
import com.lexho.user_service.customer.dto.CustomerCartItemResponse;
import com.lexho.user_service.customer.dto.CustomerCartResponse;
import com.lexho.user_service.customer.dto.CustomerUpdateCartItemRequest;
import com.lexho.user_service.customer.service.CustomerCartService;
import com.lexho.user_service.customer.service.CustomerCartSlotService;
import com.lexho.user_service.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CustomerCartFallbackController {

    private final CustomerCartService customerCartService;
    private final CustomerCartSlotService customerCartSlotService;

    @GetMapping
    public ApiResponse<CustomerCartResponse> getCart(@RequestAttribute("userId") Long userId) {
        return ApiResponse.success("Cart fetched", customerCartService.getCart(userId));
    }

    @PostMapping({"/add", ""})
    public CustomerCartItemResponse addToCart(@RequestAttribute("userId") Long userId,
                                              @Valid @RequestBody CustomerAddToCartRequest request) {
        return customerCartService.addToCart(userId, request);
    }

    @GetMapping("/config")
    public ApiResponse<CartConfigResponse> cartConfig() {
        return ApiResponse.success("Cart config fetched", customerCartService.config());
    }

    @PutMapping("/item")
    public ApiResponse<CustomerCartResponse> updateCartItem(@RequestAttribute("userId") Long userId,
                                                            @Valid @RequestBody CustomerUpdateCartItemRequest request) {
        return ApiResponse.success("Cart item updated", customerCartService.updateQuantity(userId, request));
    }

    @DeleteMapping("/item/{itemId}")
    public ApiResponse<Void> removeCartItem(@RequestAttribute("userId") Long userId, @PathVariable Long itemId) {
        customerCartService.removeItem(userId, itemId);
        return ApiResponse.success("Cart item removed", null);
    }

    @DeleteMapping("/clear")
    public ApiResponse<Void> clearCart(@RequestAttribute("userId") Long userId) {
        customerCartService.clearCart(userId);
        return ApiResponse.success("Cart cleared", null);
    }

    @PatchMapping("/meta")
    public ApiResponse<CustomerCartResponse> updateCartMeta(@RequestAttribute("userId") Long userId,
                                                            @RequestBody(required = false) Map<String, Object> payload) {
        return ApiResponse.success("Cart meta updated", customerCartService.getCart(userId));
    }

    @GetMapping("/slots")
    public ApiResponse<Map<String, Object>> cartSlots(@RequestParam LocalDate date) {
        return ApiResponse.success("Slots fetched", customerCartSlotService.slotsForDate(date));
    }
}
