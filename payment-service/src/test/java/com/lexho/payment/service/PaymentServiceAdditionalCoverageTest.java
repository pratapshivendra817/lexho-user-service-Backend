package com.lexho.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.booking.service.OrderLifecycleService;
import com.lexho.common.enums.PaymentStatus;
import com.lexho.payment.config.RazorpayProperties;
import com.lexho.payment.dto.VerifyPaymentRequest;
import com.lexho.payment.entity.PaymentRefund;
import com.lexho.payment.entity.PaymentRefundStatus;
import com.lexho.payment.entity.PaymentTransaction;
import com.lexho.payment.entity.PaymentTransactionStatus;
import com.lexho.payment.entity.PaymentWebhookEvent;
import com.lexho.payment.entity.PaymentWebhookEventStatus;
import com.lexho.payment.event.PaymentRefundProcessedEvent;
import com.lexho.payment.exception.PaymentException;
import com.lexho.payment.repository.PaymentRefundRepository;
import com.lexho.payment.repository.PaymentTransactionRepository;
import com.lexho.payment.repository.PaymentWebhookEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceAdditionalCoverageTest {

    @Mock OrderRepository orderRepository;
    @Mock PaymentTransactionRepository transactionRepository;
    @Mock PaymentRefundRepository refundRepository;
    @Mock PaymentWebhookEventRepository webhookEventRepository;
    @Mock RazorpayGateway gateway;
    @Mock PaymentSignatureVerifier signatureVerifier;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock PaymentMetricsService metricsService;

    private PaymentService service;

    @BeforeEach
    void setUp() {
        RazorpayProperties properties = new RazorpayProperties();
        properties.setKeyId("key-id");
        properties.setKeySecret("key-secret");
        properties.setWebhookSecret("webhook-secret");
        service = new PaymentService(orderRepository, transactionRepository, refundRepository,
                webhookEventRepository, gateway, properties, signatureVerifier, new ObjectMapper(),
                eventPublisher, metricsService, new OrderLifecycleService(orderRepository));
        lenient().when(gateway.toPaise(any(BigDecimal.class)))
                .thenAnswer(invocation -> invocation.<BigDecimal>getArgument(0).movePointRight(2).longValueExact());
    }

    @Test
    void retryOrderGeneratesRetryIdempotencyKeyWhenHeaderIsBlank() {
        Order booking = booking(10L, 7L, 100.0, PaymentStatus.PENDING);
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(booking));
        when(transactionRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(gateway.createOrder(10L, new BigDecimal("100.00")))
                .thenReturn(new RazorpayGateway.RazorpayOrder("order_retry", 10000L, "INR"));

        service.retryOrder(7L, 10L, " ");

        ArgumentCaptor<PaymentTransaction> transactionCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        assertTrue(transactionCaptor.getValue().getIdempotencyKey().startsWith("retry-10-"));
        assertEquals(PaymentStatus.PENDING, booking.getPaymentStatus());
    }

    @Test
    void verifyReturnsPendingForAuthorizedButUncapturedPayment() {
        PaymentTransaction transaction = transaction(10L, 7L, PaymentTransactionStatus.CREATED);
        Order booking = booking(10L, 7L, 100.0, PaymentStatus.PENDING);
        booking.setBookingType("SUBSCRIPTION");
        when(transactionRepository.findByRazorpayOrderIdForUpdate("order_1")).thenReturn(Optional.of(transaction));
        when(signatureVerifier.verifyPayment("order_1", "pay_1", "sig", "key-secret")).thenReturn(true);
        when(gateway.fetchPayment("pay_1"))
                .thenReturn(new RazorpayGateway.RazorpayPayment("pay_1", "order_1", 10000L, "authorized"));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(booking));

        var response = service.verify(7L, new VerifyPaymentRequest("order_1", "pay_1", "sig"));

        assertTrue(response.verified());
        assertEquals("PENDING", response.paymentStatus());
        assertEquals("PENDING", response.subscriptionStatus());
        assertEquals("pay_1", transaction.getRazorpayPaymentId());
        assertEquals("sig", transaction.getRazorpaySignature());
        verify(metricsService, never()).incrementPaymentSuccess();
    }

    @Test
    void processWebhookReturnsDuplicateForInFlightEvent() {
        String body = "{\"event\":\"payment.failed\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_1\"}}}}";
        PaymentWebhookEvent existing = new PaymentWebhookEvent();
        existing.setEventType("payment.failed");
        existing.setStatus(PaymentWebhookEventStatus.PROCESSING);
        when(signatureVerifier.verifyWebhook(body, "sig", "webhook-secret")).thenReturn(true);
        when(webhookEventRepository.findByIdempotencyKeyForUpdate("payment.failed:pay_1"))
                .thenReturn(Optional.of(existing));

        var result = service.processWebhook("evt_1", "sig", body);

        assertTrue(result.duplicate());
        assertEquals("payment.failed", result.eventType());
    }

    @Test
    void processWebhookHandlesAuthorizedEvent() {
        String body = """
                {"event":"payment.authorized","payload":{"payment":{"entity":{"id":"pay_1","order_id":"order_1"}}}}
                """;
        PaymentTransaction transaction = transaction(10L, 7L, null);
        when(signatureVerifier.verifyWebhook(body, "sig", "webhook-secret")).thenReturn(true);
        when(webhookEventRepository.findByIdempotencyKeyForUpdate("event:evt_2")).thenReturn(Optional.empty());
        when(transactionRepository.findByRazorpayOrderId("order_1")).thenReturn(Optional.of(transaction));

        var result = service.processWebhook("evt_2", "sig", body);

        assertTrue(result.processed());
        assertEquals("pay_1", transaction.getRazorpayPaymentId());
        assertEquals(PaymentTransactionStatus.CREATED, transaction.getPaymentStatus());
    }

    @Test
    void processWebhookHandlesCapturedEventIdempotentlyWhenAlreadyPaid() {
        String body = """
                {"event":"payment.captured","payload":{"payment":{"entity":{"id":"pay_1","order_id":"order_1"}}}}
                """;
        PaymentTransaction transaction = transaction(10L, 7L, PaymentTransactionStatus.PAID);
        when(signatureVerifier.verifyWebhook(body, "sig", "webhook-secret")).thenReturn(true);
        when(webhookEventRepository.findByIdempotencyKeyForUpdate("payment.captured:pay_1")).thenReturn(Optional.empty());
        when(transactionRepository.findByRazorpayOrderIdForUpdate("order_1")).thenReturn(Optional.of(transaction));

        var result = service.processWebhook("evt_3", "sig", body);

        assertTrue(result.processed());
        verify(metricsService, never()).incrementPaymentSuccess();
    }

    @Test
    void processWebhookHandlesFailedPaymentAndMarksBookingFailed() {
        String body = """
                {"event":"payment.failed","payload":{"payment":{"entity":{"id":"pay_9","order_id":"order_1","error_description":"bank rejected"}}}}
                """;
        PaymentTransaction transaction = transaction(10L, 7L, PaymentTransactionStatus.CREATED);
        Order booking = booking(10L, 7L, 100.0, PaymentStatus.PENDING);
        when(signatureVerifier.verifyWebhook(body, "sig", "webhook-secret")).thenReturn(true);
        when(webhookEventRepository.findByIdempotencyKeyForUpdate("payment.failed:pay_9")).thenReturn(Optional.empty());
        when(transactionRepository.findByRazorpayOrderIdForUpdate("order_1")).thenReturn(Optional.of(transaction));
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(booking));

        var result = service.processWebhook("evt_4", "sig", body);

        assertTrue(result.processed());
        assertEquals(PaymentTransactionStatus.FAILED, transaction.getPaymentStatus());
        assertEquals("bank rejected", transaction.getFailureReason());
        assertEquals(PaymentStatus.FAILED, booking.getPaymentStatus());
        verify(metricsService).incrementPaymentFailed();
    }

    @Test
    void processWebhookCreatesRefundRequestFromRefundCreatedEvent() {
        String body = """
                {"event":"refund.created","payload":{"refund":{"entity":{"id":"rf_1","payment_id":"pay_1","amount":1250}}}}
                """;
        PaymentTransaction transaction = transaction(10L, 7L, PaymentTransactionStatus.PAID);
        setField(transaction, "id", 55L);
        transaction.setRazorpayPaymentId("pay_1");
        when(signatureVerifier.verifyWebhook(body, "sig", "webhook-secret")).thenReturn(true);
        when(transactionRepository.findByRazorpayPaymentId("pay_1")).thenReturn(Optional.of(transaction));
        when(webhookEventRepository.findByIdempotencyKeyForUpdate("event:evt_5")).thenReturn(Optional.empty());
        when(refundRepository.findByRazorpayRefundId("rf_1")).thenReturn(Optional.empty());

        var result = service.processWebhook("evt_5", "sig", body);

        assertTrue(result.processed());
        ArgumentCaptor<PaymentRefund> refundCaptor = ArgumentCaptor.forClass(PaymentRefund.class);
        verify(refundRepository).save(refundCaptor.capture());
        assertEquals(PaymentRefundStatus.REQUESTED, refundCaptor.getValue().getRefundStatus());
        assertEquals(new BigDecimal("12.50"), refundCaptor.getValue().getAmount());
    }

    @Test
    void processWebhookMarksTransactionRefundedWhenProcessedRefundMatchesFullAmount() {
        String body = """
                {"event":"refund.processed","payload":{"refund":{"entity":{"id":"rf_2","payment_id":"pay_2","amount":10000}}}}
                """;
        PaymentTransaction transaction = transaction(10L, 7L, PaymentTransactionStatus.PAID);
        setField(transaction, "id", 77L);
        transaction.setRazorpayPaymentId("pay_2");
        Order booking = booking(10L, 7L, 100.0, PaymentStatus.SUCCESS);
        when(signatureVerifier.verifyWebhook(body, "sig", "webhook-secret")).thenReturn(true);
        when(webhookEventRepository.findByIdempotencyKeyForUpdate("event:evt_6")).thenReturn(Optional.empty());
        when(transactionRepository.findByRazorpayPaymentIdForUpdate("pay_2")).thenReturn(Optional.of(transaction));
        when(refundRepository.findByRazorpayRefundId("rf_2")).thenReturn(Optional.empty());
        when(refundRepository.sumAmountByTransactionIdAndStatuses(eq(77L), anyList()))
                .thenReturn(new BigDecimal("100.00"));
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(booking));

        var result = service.processWebhook("evt_6", "sig", body);

        assertTrue(result.processed());
        assertEquals(PaymentTransactionStatus.REFUNDED, transaction.getPaymentStatus());
        assertEquals(PaymentStatus.REFUNDED, booking.getPaymentStatus());
        verify(eventPublisher).publishEvent(any(PaymentRefundProcessedEvent.class));
    }

    @Test
    void processWebhookLeavesTransactionPaidWhenProcessedRefundIsPartial() {
        String body = """
                {"event":"refund.processed","payload":{"refund":{"entity":{"id":"rf_3","payment_id":"pay_3","amount":2500}}}}
                """;
        PaymentTransaction transaction = transaction(10L, 7L, PaymentTransactionStatus.PAID);
        setField(transaction, "id", 88L);
        transaction.setRazorpayPaymentId("pay_3");
        when(signatureVerifier.verifyWebhook(body, "sig", "webhook-secret")).thenReturn(true);
        when(webhookEventRepository.findByIdempotencyKeyForUpdate("event:evt_7")).thenReturn(Optional.empty());
        when(transactionRepository.findByRazorpayPaymentIdForUpdate("pay_3")).thenReturn(Optional.of(transaction));
        when(refundRepository.findByRazorpayRefundId("rf_3")).thenReturn(Optional.empty());
        when(refundRepository.sumAmountByTransactionIdAndStatuses(eq(88L), anyList()))
                .thenReturn(new BigDecimal("25.00"));

        var result = service.processWebhook("evt_7", "sig", body);

        assertTrue(result.processed());
        assertEquals(PaymentTransactionStatus.PAID, transaction.getPaymentStatus());
    }

    @Test
    void processWebhookThrowsForInvalidJsonPayload() {
        when(signatureVerifier.verifyWebhook("not-json", "sig", "webhook-secret")).thenReturn(true);

        PaymentException exception = assertThrows(PaymentException.class,
                () -> service.processWebhook("evt_bad_json", "sig", "not-json"));

        assertEquals("Invalid webhook payload", exception.getMessage());
    }

    @Test
    void processWebhookSchedulesRetryAndEventuallyMovesToDeadLetter() {
        String body = """
                {"event":"refund.processed","payload":{"refund":{"entity":{"id":"rf_dead","payment_id":"missing","amount":100}}}}
                """;
        when(signatureVerifier.verifyWebhook(body, "sig", "webhook-secret")).thenReturn(true);
        when(transactionRepository.findByRazorpayPaymentId("missing")).thenReturn(Optional.empty());
        when(transactionRepository.findByRazorpayPaymentIdForUpdate("missing")).thenReturn(Optional.empty());

        when(webhookEventRepository.findByIdempotencyKeyForUpdate("event:evt_retry")).thenReturn(Optional.empty());
        var first = service.processWebhook("evt_retry", "sig", body);

        assertTrue(first.retryScheduled());
        ArgumentCaptor<PaymentWebhookEvent> firstSaveCaptor = ArgumentCaptor.forClass(PaymentWebhookEvent.class);
        verify(webhookEventRepository, atLeast(2)).save(firstSaveCaptor.capture());
        PaymentWebhookEvent failed = firstSaveCaptor.getAllValues().get(firstSaveCaptor.getAllValues().size() - 1);
        assertEquals(PaymentWebhookEventStatus.FAILED, failed.getStatus());
        assertNotNull(failed.getNextRetryAt());
        assertEquals(1, failed.getRetryCount());

        PaymentWebhookEvent existing = new PaymentWebhookEvent();
        existing.setEventId("evt_dead");
        existing.setEventType("refund.processed");
        existing.setIdempotencyKey("event:evt_dead");
        existing.setRetryCount(5);
        existing.setStatus(PaymentWebhookEventStatus.FAILED);
        existing.setRawPayload(body);
        when(webhookEventRepository.findByIdempotencyKeyForUpdate("event:evt_dead")).thenReturn(Optional.of(existing));

        var second = service.processWebhook("evt_dead", "sig", body);

        assertTrue(second.retryScheduled());
        assertEquals(PaymentWebhookEventStatus.DEAD_LETTER, existing.getStatus());
        assertNull(existing.getNextRetryAt());
        assertEquals(6, existing.getRetryCount());
    }

    @Test
    void retryDueWebhookEventsProcessesPendingCandidates() {
        PaymentWebhookEvent event = new PaymentWebhookEvent();
        event.setEventId("evt_pending");
        event.setEventType("payment.authorized");
        event.setStatus(PaymentWebhookEventStatus.PENDING);
        event.setRawPayload("{\"event\":\"payment.authorized\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_1\",\"order_id\":\"order_1\"}}}}");
        PaymentTransaction transaction = transaction(10L, 7L, PaymentTransactionStatus.CREATED);
        when(webhookEventRepository.findRetryCandidates(anyList(), any(LocalDateTime.class))).thenReturn(List.of(event));
        when(webhookEventRepository.findByEventIdForUpdate("evt_pending")).thenReturn(Optional.of(event));
        when(transactionRepository.findByRazorpayOrderId("order_1")).thenReturn(Optional.of(transaction));

        service.retryDueWebhookEvents();

        assertEquals(PaymentWebhookEventStatus.SUCCESS, event.getStatus());
        assertEquals("pay_1", transaction.getRazorpayPaymentId());
    }

    private static Order booking(Long id, Long userId, Double amount, PaymentStatus paymentStatus) {
        Order booking = new Order();
        booking.setId(id);
        booking.setUserId(userId);
        booking.setTotalAmount(amount);
        booking.setPaymentStatus(paymentStatus);
        return booking;
    }

    private static PaymentTransaction transaction(Long bookingId, Long customerId, PaymentTransactionStatus status) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setBookingId(bookingId);
        transaction.setCustomerId(customerId);
        transaction.setIdempotencyKey("idem");
        transaction.setRazorpayOrderId("order_1");
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setCurrency("INR");
        transaction.setPaymentStatus(status);
        return transaction;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
