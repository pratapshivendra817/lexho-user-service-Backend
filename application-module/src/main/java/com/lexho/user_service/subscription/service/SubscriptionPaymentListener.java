package com.lexho.user_service.subscription.service;

import com.lexho.payment.event.PaymentCapturedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionPaymentListener {
    private final SubscriptionBookingService subscriptionBookingService;
    private final SubscriptionAutoDispatchService subscriptionAutoDispatchService;

    public SubscriptionPaymentListener(SubscriptionBookingService subscriptionBookingService,
                                      SubscriptionAutoDispatchService subscriptionAutoDispatchService) {
        this.subscriptionBookingService = subscriptionBookingService;
        this.subscriptionAutoDispatchService = subscriptionAutoDispatchService;
    }

    @EventListener
    public void onPaymentCaptured(PaymentCapturedEvent event) {
        subscriptionBookingService.activateByBookingId(event.bookingId());
        subscriptionAutoDispatchService.dispatchForBooking(event.bookingId());
    }
}
