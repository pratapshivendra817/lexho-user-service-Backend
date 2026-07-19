package com.lexho.payment.service;

import com.lexho.payment.config.RazorpayProperties;
import com.lexho.payment.exception.PaymentException;
import com.lexho.payment.webhook.PaymentWebhookRetryScheduler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PaymentSupportComponentsTest {

    @Test
    void paymentMetricsServiceIncrementsSuccessAndFailureCounters() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        PaymentMetricsService metricsService = new PaymentMetricsService(registry);

        metricsService.incrementPaymentSuccess();
        metricsService.incrementPaymentSuccess();
        metricsService.incrementPaymentFailed();

        assertEquals(2.0, registry.get("payment_success").counter().count());
        assertEquals(1.0, registry.get("payment_failed").counter().count());
    }

    @Test
    void retrySchedulerDelegatesToPaymentService() {
        PaymentService paymentService = mock(PaymentService.class);
        PaymentWebhookRetryScheduler scheduler = new PaymentWebhookRetryScheduler(paymentService);

        scheduler.retryFailedWebhookEvents();

        verify(paymentService).retryDueWebhookEvents();
    }

    @Test
    void razorpayGatewayRejectsMissingCredentialsForEveryExternalCall() {
        RazorpayProperties properties = new RazorpayProperties();
        RazorpayGateway gateway = new RazorpayGateway(properties);

        PaymentException orderError = assertThrows(PaymentException.class,
                () -> gateway.createOrder(10L, new BigDecimal("100.00")));
        PaymentException paymentError = assertThrows(PaymentException.class,
                () -> gateway.fetchPayment("pay_1"));
        PaymentException refundError = assertThrows(PaymentException.class,
                () -> gateway.createRefund("pay_1", new BigDecimal("10.00"), 10L));

        assertEquals("Razorpay credentials are not configured", orderError.getMessage());
        assertEquals("Razorpay credentials are not configured", paymentError.getMessage());
        assertEquals("Razorpay credentials are not configured", refundError.getMessage());
    }

    @Test
    void razorpayGatewayUtilityMethodsAndRecordsExposeExpectedBehavior() {
        RazorpayProperties properties = new RazorpayProperties();
        properties.setCurrency("USD");
        RazorpayGateway gateway = new RazorpayGateway(properties);

        assertEquals(1234L, gateway.toPaise(new BigDecimal("12.34")));
        assertEquals("USD", properties.getCurrency());
        assertEquals("INR", new RazorpayProperties().getCurrency());
        assertTrue(new RazorpayGateway.RazorpayPayment("pay_1", "order_1", 1000L, "CAPTURED").isCaptured());
        assertTrue(new RazorpayGateway.RazorpayRefund("rf_1", "pay_1", 500L, "processed").isProcessed());
    }
}
