package com.lexho.user_service.subscription.service;

import com.lexho.payment.event.PaymentCapturedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionPaymentListener {
    private final SubscriptionBookingService subscriptionBookingService;

    public SubscriptionPaymentListener(SubscriptionBookingService subscriptionBookingService) {
        this.subscriptionBookingService = subscriptionBookingService;
    }

    @EventListener
    public void onPaymentCaptured(PaymentCapturedEvent event) {
        subscriptionBookingService.activateByBookingId(event.bookingId());
    }
}
