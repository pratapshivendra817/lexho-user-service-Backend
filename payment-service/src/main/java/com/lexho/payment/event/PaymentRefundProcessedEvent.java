package com.lexho.payment.event;

import java.math.BigDecimal;

public record PaymentRefundProcessedEvent(Long bookingId, String razorpayRefundId, BigDecimal amount) {
}
