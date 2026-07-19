package com.lexho.payment.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdminPaymentReconciliationResponse(
        Long bookingId,
        Double bookingAmount,
        String bookingStatus,
        String bookingPaymentStatus,
        BigDecimal processedRefundAmount,
        List<AdminPaymentTransactionResponse> transactions,
        List<AdminRefundResponse> refunds
) {
    public AdminPaymentReconciliationResponse {
        transactions = List.copyOf(transactions);
        refunds = List.copyOf(refunds);
    }

    @Override
    public List<AdminPaymentTransactionResponse> transactions() {
        return List.copyOf(transactions);
    }

    @Override
    public List<AdminRefundResponse> refunds() {
        return List.copyOf(refunds);
    }
}
