package com.lexho.user_service.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.dto.CartRequest;
import com.lexho.user_service.dto.CartResponseDTO;
import com.lexho.user_service.dto.CartSummaryDTO;
import com.lexho.user_service.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping
    public ApiResponse<CartResponseDTO> addToCart(@RequestAttribute("userId") Long userId,
                                                  @RequestBody CartRequest request) {
        return ApiResponse.success("Added to cart", cartService.addToCart(userId, request));
    }

    @GetMapping
    public ApiResponse<CartSummaryDTO> getCart(@RequestAttribute("userId") Long userId) {
        return ApiResponse.success("Cart fetched", cartService.getCart(userId));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> remove(@RequestAttribute("userId") Long userId,
                                      @PathVariable Long id) {
        return ApiResponse.success(cartService.removeFromCart(userId, id), null);
    }
}

