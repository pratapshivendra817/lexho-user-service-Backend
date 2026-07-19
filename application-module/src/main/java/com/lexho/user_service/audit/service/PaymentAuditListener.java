package com.lexho.user_service.audit.service;

import com.lexho.payment.event.PaymentCapturedEvent;
import com.lexho.payment.event.PaymentFailedEvent;
import com.lexho.payment.event.PaymentRefundProcessedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentAuditListener {

    private final AuditLogService auditLogService;

    public PaymentAuditListener(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @EventListener
    public void onPaymentCaptured(PaymentCapturedEvent event) {
        auditLogService.log(null, null, event.bookingId(), "PAYMENT_SUCCESS", null, "SUCCESS");
    }

    @EventListener
    public void onRefundProcessed(PaymentRefundProcessedEvent event) {
        auditLogService.log(null, null, event.bookingId(), "PAYMENT_REFUNDED", null, event.razorpayRefundId());
    }

    @EventListener
    public void onPaymentFailed(PaymentFailedEvent event) {
        auditLogService.log(null, null, event.bookingId(), "PAYMENT_FAILED", null, event.reason());
    }
}
