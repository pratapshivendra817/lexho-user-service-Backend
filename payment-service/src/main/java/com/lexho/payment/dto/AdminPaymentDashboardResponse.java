package com.lexho.payment.dto;

import java.math.BigDecimal;

public record AdminPaymentDashboardResponse(
        long totalAttempts,
        long createdAttempts,
        long paidAttempts,
        long failedAttempts,
        long refundedAttempts,
        BigDecimal paidAmount,
        BigDecimal processedRefundAmount
) {
}
