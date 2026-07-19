package com.lexho.payment.webhook;

import com.lexho.payment.service.PaymentService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PaymentWebhookRetryScheduler {

    private final PaymentService paymentService;

    public PaymentWebhookRetryScheduler(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Scheduled(fixedDelayString = "${app.payments.webhook.retry-scheduler-delay-ms:60000}")
    public void retryFailedWebhookEvents() {
        paymentService.retryDueWebhookEvents();
    }
}
