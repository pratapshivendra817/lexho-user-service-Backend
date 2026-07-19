package com.lexho.payment.dto;

import java.util.List;

public record AdminPaymentTransactionPageResponse(
        List<AdminPaymentTransactionResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public AdminPaymentTransactionPageResponse {
        items = List.copyOf(items);
    }

    @Override
    public List<AdminPaymentTransactionResponse> items() {
        return List.copyOf(items);
    }
}
