package com.lexho.payment.entity;

public enum PaymentWebhookEventStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    DEAD_LETTER
}
