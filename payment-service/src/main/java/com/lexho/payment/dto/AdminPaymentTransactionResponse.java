package com.lexho.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminPaymentTransactionResponse(
        Long id,
        Long bookingId,
        Long customerId,
        String razorpayOrderId,
        String razorpayPaymentId,
        BigDecimal amount,
        String currency,
        String paymentStatus,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime paidAt
) {
}
