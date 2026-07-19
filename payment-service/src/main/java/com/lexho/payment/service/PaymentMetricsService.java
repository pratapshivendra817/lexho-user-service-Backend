package com.lexho.payment.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class PaymentMetricsService {

    private final Counter paymentSuccess;
    private final Counter paymentFailed;

    public PaymentMetricsService(MeterRegistry meterRegistry) {
        this.paymentSuccess = Counter.builder("payment_success")
                .description("Successful payment events processed")
                .register(meterRegistry);
        this.paymentFailed = Counter.builder("payment_failed")
                .description("Failed payment events processed")
                .register(meterRegistry);
    }

    public void incrementPaymentSuccess() {
        paymentSuccess.increment();
    }

    public void incrementPaymentFailed() {
        paymentFailed.increment();
    }
}
