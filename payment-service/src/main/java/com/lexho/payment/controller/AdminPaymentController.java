package com.lexho.payment.controller;

import com.lexho.payment.dto.AdminPaymentDashboardResponse;
import com.lexho.payment.dto.AdminPaymentReconciliationResponse;
import com.lexho.payment.dto.AdminPaymentTransactionPageResponse;
import com.lexho.payment.dto.AdminRefundRequest;
import com.lexho.payment.dto.AdminRefundResponse;
import com.lexho.payment.dto.PaymentApiResponse;
import com.lexho.payment.service.AdminPaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/payments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentController {

    private final AdminPaymentService adminPaymentService;

    public AdminPaymentController(AdminPaymentService adminPaymentService) {
        this.adminPaymentService = adminPaymentService;
    }

    @GetMapping("/dashboard")
    public PaymentApiResponse<AdminPaymentDashboardResponse> dashboard() {
        return PaymentApiResponse.success("Payment dashboard fetched", adminPaymentService.dashboard());
    }

    @GetMapping("/transactions")
    public PaymentApiResponse<AdminPaymentTransactionPageResponse> transactions(
            @RequestParam(required = false) Long bookingId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PaymentApiResponse.success(
                "Payment transactions fetched",
                adminPaymentService.transactions(bookingId, status, page, size)
        );
    }

    @GetMapping("/bookings/{bookingId}")
    public PaymentApiResponse<AdminPaymentReconciliationResponse> reconciliation(@PathVariable Long bookingId) {
        return PaymentApiResponse.success(
                "Payment reconciliation fetched",
                adminPaymentService.reconciliation(bookingId)
        );
    }

    @PostMapping("/bookings/{bookingId}/reconcile")
    public PaymentApiResponse<AdminPaymentReconciliationResponse> reconcileLatest(@PathVariable Long bookingId) {
        return PaymentApiResponse.success(
                "Payment reconciled",
                adminPaymentService.reconcileLatest(bookingId)
        );
    }

    @PostMapping("/bookings/{bookingId}/refunds")
    public PaymentApiResponse<AdminRefundResponse> createRefund(
            @RequestAttribute("userId") Long adminId,
            @PathVariable Long bookingId,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody AdminRefundRequest request) {
        return PaymentApiResponse.success(
                "Refund request submitted",
                adminPaymentService.createRefund(adminId, bookingId, idempotencyKey, request)
        );
    }
}
