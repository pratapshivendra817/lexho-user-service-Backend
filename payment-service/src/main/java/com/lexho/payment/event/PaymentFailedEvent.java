package com.lexho.payment.event;

public record PaymentFailedEvent(Long bookingId, String reason) {
}
