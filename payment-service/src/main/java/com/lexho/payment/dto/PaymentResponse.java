package com.lexho.payment.dto;

import java.math.BigDecimal;
public record PaymentResponse(
        Long bookingId,
        String razorpayOrderId,
        String razorpayPaymentId,
        BigDecimal amount,
        String currency,
        String paymentStatus,
        String status,
        String subscriptionStatus,
        boolean retryPaymentAllowed,
        boolean verified,
        String createdAt,
        String paidAt
) {
}
