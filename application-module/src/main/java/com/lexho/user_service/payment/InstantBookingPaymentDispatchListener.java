package com.lexho.user_service.payment;

import com.lexho.payment.event.PaymentCapturedEvent;
import com.lexho.payment.event.PaymentFailedEvent;
import com.lexho.user_service.dispatch.service.DispatchOrchestrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class InstantBookingPaymentDispatchListener {

    private static final Logger LOG = LoggerFactory.getLogger(InstantBookingPaymentDispatchListener.class);

    private final DispatchOrchestrationService dispatchOrchestrationService;

    public InstantBookingPaymentDispatchListener(DispatchOrchestrationService dispatchOrchestrationService) {
        this.dispatchOrchestrationService = dispatchOrchestrationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCaptured(PaymentCapturedEvent event) {
        int created = dispatchOrchestrationService.dispatchPaidInstantBooking(event.bookingId());
        int scheduledCreated = dispatchOrchestrationService.dispatchPaidScheduledBooking(event.bookingId());
        LOG.info("Payment dispatch gate processed bookingId={} instantRequests={} scheduledRequests={}",
                event.bookingId(), created, scheduledCreated);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentFailed(PaymentFailedEvent event) {
        dispatchOrchestrationService.handlePaymentFailed(event.bookingId(), event.reason());
        LOG.info("Payment failure cleanup processed bookingId={}", event.bookingId());
    }
}
