package com.lexho.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminRefundResponse(
        Long id,
        Long bookingId,
        String razorpayRefundId,
        BigDecimal amount,
        String currency,
        String refundStatus,
        String reason,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime processedAt
) {
}
