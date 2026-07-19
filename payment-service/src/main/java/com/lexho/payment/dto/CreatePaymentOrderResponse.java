package com.lexho.payment.dto;

import java.math.BigDecimal;

public record CreatePaymentOrderResponse(
        Long bookingId,
        String razorpayOrderId,
        String keyId,
        Long amountInPaise,
        BigDecimal amount,
        String currency,
        String status
) {
}
