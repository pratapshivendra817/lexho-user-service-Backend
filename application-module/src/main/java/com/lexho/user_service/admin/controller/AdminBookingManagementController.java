package com.lexho.user_service.admin.controller;

import com.lexho.common.enums.OrderStatus;
import com.lexho.common.enums.PaymentStatus;
import com.lexho.user_service.admin.service.AdminBookingManagementService;
import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.map.dto.NavigationRouteResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/booking-management")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookingManagementController {

    private final AdminBookingManagementService adminBookingManagementService;

    public AdminBookingManagementController(AdminBookingManagementService adminBookingManagementService) {
        this.adminBookingManagementService = adminBookingManagementService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(required = false) String search,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size,
                                                  @RequestParam(defaultValue = "createdAt") String sortBy,
                                                  @RequestParam(defaultValue = "desc") String sortDir) {
        return ApiResponse.success("Bookings fetched",
                adminBookingManagementService.list(search, status, page, size, sortBy, sortDir));
    }

    @GetMapping("/{bookingId}")
    public ApiResponse<Map<String, Object>> byId(@PathVariable Long bookingId) {
        return ApiResponse.success("Booking fetched", adminBookingManagementService.byId(bookingId));
    }

    @PatchMapping("/{bookingId}/status")
    public ApiResponse<Map<String, Object>> updateStatus(@PathVariable Long bookingId,
                                                          @Valid @RequestBody BookingStatusUpdateRequest request) {
        return ApiResponse.success("Booking status updated",
                adminBookingManagementService.updateStatus(bookingId, request.status));
    }

    @PatchMapping("/{bookingId}/cancel")
    public ApiResponse<Map<String, Object>> cancel(@PathVariable Long bookingId) {
        return ApiResponse.success("Booking cancelled", adminBookingManagementService.cancel(bookingId));
    }

    @GetMapping("/{bookingId}/tracking/live")
    public ApiResponse<Map<String, Object>> liveTracking(@PathVariable Long bookingId) {
        return ApiResponse.success("Live tracking fetched", adminBookingManagementService.liveTracking(bookingId));
    }

    @GetMapping("/{bookingId}/navigation")
    public ApiResponse<NavigationRouteResponse> navigation(@PathVariable Long bookingId) {
        return ApiResponse.success("Navigation ETA fetched", adminBookingManagementService.navigation(bookingId));
    }

    @PatchMapping("/{bookingId}/tracking/partner-location")
    public ApiResponse<Map<String, Object>> overridePartnerLocation(@PathVariable Long bookingId,
                                                                    @Valid @RequestBody PartnerLocationOverrideRequest request) {
        return ApiResponse.success("Partner location overridden",
                adminBookingManagementService.overridePartnerLocation(bookingId, request.latitude, request.longitude, request.pincode));
    }

    @GetMapping("/{bookingId}/payment/reconciliation")
    public ApiResponse<Map<String, Object>> paymentReconciliation(@PathVariable Long bookingId) {
        return ApiResponse.success("Payment reconciliation fetched",
                adminBookingManagementService.paymentReconciliation(bookingId));
    }

    @PatchMapping("/{bookingId}/payment/status")
    public ApiResponse<Map<String, Object>> updatePaymentStatus(@PathVariable Long bookingId,
                                                                 @RequestAttribute("userId") Long adminId,
                                                                 @Valid @RequestBody PaymentStatusUpdateRequest request) {
        return ApiResponse.success("Payment status updated",
                adminBookingManagementService.updatePaymentStatus(
                        bookingId,
                        request.paymentStatus,
                        request.reason,
                        adminId
                ));
    }

    @PutMapping("/{bookingId}/payment/payout")
    public ApiResponse<Map<String, Object>> upsertPayout(@PathVariable Long bookingId,
                                                          @Valid @RequestBody PayoutUpsertRequest request) {
        return ApiResponse.success("Payout upserted",
                adminBookingManagementService.createOrUpdatePayout(bookingId, request.providerId, request.amount, request.status));
    }

    public static class BookingStatusUpdateRequest {
        @NotNull(message = "status is required")
        public OrderStatus status;
    }

    public static class PartnerLocationOverrideRequest {
        @NotNull(message = "latitude is required")
        public Double latitude;
        @NotNull(message = "longitude is required")
        public Double longitude;
        public String pincode;
    }

    public static class PaymentStatusUpdateRequest {
        @NotNull(message = "paymentStatus is required")
        public PaymentStatus paymentStatus;
        @NotBlank(message = "reason is required")
        public String reason;
    }

    public static class PayoutUpsertRequest {
        @NotNull(message = "providerId is required")
        public Long providerId;
        @NotNull(message = "amount is required")
        public Double amount;
        public String status;
    }
}
