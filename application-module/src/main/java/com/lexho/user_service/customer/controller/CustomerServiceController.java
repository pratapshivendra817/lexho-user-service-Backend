package com.lexho.user_service.customer.controller;

import com.lexho.user_service.cartconfig.dto.CartConfigResponse;
import com.lexho.user_service.customer.dto.CustomerAddToCartRequest;
import com.lexho.user_service.customer.dto.CustomerCartResponse;
import com.lexho.user_service.customer.dto.CustomerCartItemResponse;
import com.lexho.user_service.customer.dto.CookFamilySizeResponse;
import com.lexho.user_service.customer.dto.CustomerPopularServiceResponse;
import com.lexho.user_service.customer.dto.CustomerServiceCatalogPageResponse;
import com.lexho.user_service.customer.dto.CustomerServiceCatalogResponse;
import com.lexho.user_service.customer.dto.CustomerServiceCardResponse;
import com.lexho.user_service.customer.dto.CustomerUpdateCartItemRequest;
import com.lexho.user_service.customer.service.CustomerCartService;
import com.lexho.user_service.customer.service.CustomerCartSlotService;
import com.lexho.user_service.customer.service.CustomerCatalogService;
import com.lexho.user_service.customer.service.CustomerPopularServiceService;
import com.lexho.user_service.customer.service.CustomerServiceCatalogService;
import com.lexho.user_service.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerServiceController {

    private final CustomerCatalogService customerCatalogService;
    private final CustomerCartService customerCartService;
    private final CustomerCartSlotService customerCartSlotService;
    private final CustomerPopularServiceService customerPopularServiceService;
    private final CustomerServiceCatalogService customerServiceCatalogService;

    @GetMapping("/services")
    public ApiResponse<List<CustomerServiceCardResponse>> getServicesByCategory(
            @RequestParam Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success("Services fetched",
                customerCatalogService.getServicesByCategory(categoryId, page, size));
    }

    @GetMapping("/services/popular")
    @Operation(summary = "Get popular services by category")
    public ApiResponse<List<CustomerPopularServiceResponse>> getPopularServicesByCategory(@RequestParam Long categoryId,
                                                                                           @RequestParam(defaultValue = "0") int page,
                                                                                           @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success("Popular services fetched",
                customerPopularServiceService.getPopularServices(categoryId, page, size));
    }

    @GetMapping("/services/popular/{category}")
    @Operation(summary = "Get popular services by category key")
    public ApiResponse<List<CustomerPopularServiceResponse>> getPopularServicesByCategoryKey(@PathVariable String category) {
        return ApiResponse.success("Popular services fetched", customerPopularServiceService.getPopularServicesByCategoryKey(category));
    }

    @GetMapping("/services/popular/cook")
    @Operation(summary = "Get popular cook services")
    public ApiResponse<List<CustomerPopularServiceResponse>> getPopularCookServices() {
        return ApiResponse.success("Popular cook services fetched", customerPopularServiceService.getPopularCookServices());
    }

    @GetMapping("/services/popular/cleaning")
    @Operation(summary = "Get popular cleaning services")
    public ApiResponse<List<CustomerPopularServiceResponse>> getPopularCleaningServices() {
        return ApiResponse.success("Popular cleaning services fetched", customerPopularServiceService.getPopularCleaningServices());
    }

    @GetMapping("/services/catalog")
    public ApiResponse<CustomerServiceCatalogPageResponse> serviceCatalog(@RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "20") int size,
                                                                          @RequestParam(required = false) String search) {
        return ApiResponse.success("Services fetched", customerServiceCatalogService.listServices(page, size, search));
    }

    @GetMapping("/services/{id}")
    public ApiResponse<CustomerServiceCatalogResponse> serviceById(@PathVariable Long id) {
        return ApiResponse.success("Service detail fetched", customerServiceCatalogService.getServiceById(id));
    }

    @PostMapping({"/cart", "/cart/add"})
    public CustomerCartItemResponse addToCart(@RequestAttribute("userId") Long userId,
                                              @Valid @RequestBody CustomerAddToCartRequest request) {
        return customerCartService.addToCart(userId, request);
    }

    @GetMapping({"/cart", "/cart/"})
    public ApiResponse<CustomerCartResponse> getCart(@RequestAttribute("userId") Long userId) {
        return ApiResponse.success("Cart fetched", customerCartService.getCart(userId));
    }

    @GetMapping("/cart/config")
    public ApiResponse<CartConfigResponse> cartConfig() {
        return ApiResponse.success("Cart config fetched", customerCartService.config());
    }

    @GetMapping("/cook-family-sizes")
    public ApiResponse<List<CookFamilySizeResponse>> cookFamilySizes() {
        return ApiResponse.success("Cook family sizes fetched", customerCartService.cookFamilySizes());
    }

    @PutMapping("/cart/item")
    public ApiResponse<CustomerCartResponse> updateCartItem(@RequestAttribute("userId") Long userId,
                                                            @Valid @RequestBody CustomerUpdateCartItemRequest request) {
        return ApiResponse.success("Cart item updated", customerCartService.updateQuantity(userId, request));
    }

    @DeleteMapping("/cart/item/{itemId}")
    public ApiResponse<Void> removeCartItem(@RequestAttribute("userId") Long userId, @PathVariable Long itemId) {
        customerCartService.removeItem(userId, itemId);
        return ApiResponse.success("Cart item removed", null);
    }

    @DeleteMapping("/cart/clear")
    public ApiResponse<Void> clearCart(@RequestAttribute("userId") Long userId) {
        customerCartService.clearCart(userId);
        return ApiResponse.success("Cart cleared", null);
    }

    @PatchMapping("/cart/meta")
    public ApiResponse<CustomerCartResponse> updateCartMeta(@RequestAttribute("userId") Long userId,
                                                            @RequestBody(required = false) Map<String, Object> payload) {
        return ApiResponse.success("Cart meta updated", customerCartService.getCart(userId));
    }

    @GetMapping("/cart/slots")
    public ApiResponse<Map<String, Object>> cartSlots(@RequestParam LocalDate date) {
        return ApiResponse.success("Slots fetched", customerCartSlotService.slotsForDate(date));
    }

}
