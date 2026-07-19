package com.lexho.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.OrderStatus;
import com.lexho.common.enums.PaymentStatus;
import com.lexho.payment.config.RazorpayProperties;
import com.lexho.payment.dto.VerifyPaymentRequest;
import com.lexho.payment.entity.PaymentTransaction;
import com.lexho.payment.entity.PaymentTransactionStatus;
import com.lexho.payment.entity.PaymentWebhookEvent;
import com.lexho.payment.entity.PaymentWebhookEventStatus;
import com.lexho.payment.event.PaymentCapturedEvent;
import com.lexho.payment.event.PaymentFailedEvent;
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
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock PaymentTransactionRepository transactionRepository;
    @Mock PaymentRefundRepository refundRepository;
    @Mock PaymentWebhookEventRepository webhookEventRepository;
    @Mock RazorpayGateway gateway;
    @Mock PaymentSignatureVerifier signatureVerifier;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock PaymentMetricsService metricsService;

    private RazorpayProperties properties;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        properties = new RazorpayProperties();
        properties.setKeyId("key-id");
        properties.setKeySecret("key-secret");
        properties.setWebhookSecret("webhook-secret");
        service = new PaymentService(orderRepository, transactionRepository, refundRepository,
                webhookEventRepository, gateway, properties, signatureVerifier, new ObjectMapper(),
                eventPublisher, metricsService);
        lenient().when(gateway.toPaise(any(BigDecimal.class)))
                .thenAnswer(invocation -> invocation.<BigDecimal>getArgument(0).movePointRight(2).longValueExact());
    }

    @Test
    void createOrderShouldPersistTransactionAndMarkBookingPending() {
        Order booking = booking(10L, 7L, 250.0, PaymentStatus.PENDING);
        when(transactionRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(booking));
        when(gateway.createOrder(10L, new BigDecimal("250.00")))
                .thenReturn(new RazorpayGateway.RazorpayOrder("order_1", 25000L, "INR"));

        var response = service.createOrder(7L, 10L, "idem-1");

        assertEquals("order_1", response.razorpayOrderId());
        assertEquals(25000L, response.amountInPaise());
        assertEquals("CREATED", response.status());
        ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(transactionRepository).save(captor.capture());
        assertEquals(7L, captor.getValue().getCustomerId());
        assertEquals(PaymentStatus.PENDING, booking.getPaymentStatus());
        verify(orderRepository).save(booking);
    }

    @Test
    void createOrderShouldReturnOwnedIdempotentTransactionAndRejectOtherOwner() {
        PaymentTransaction transaction = transaction(10L, 7L, PaymentTransactionStatus.CREATED);
        when(transactionRepository.findByIdempotencyKey("same")).thenReturn(Optional.of(transaction));

        assertEquals("order_1", service.createOrder(7L, 10L, "same").razorpayOrderId());
        assertThrows(PaymentException.class, () -> service.createOrder(8L, 10L, "same"));
        assertThrows(PaymentException.class, () -> service.createOrder(7L, 10L, " "));
        verifyNoInteractions(orderRepository);
    }

    @Test
    void createOrderShouldRejectMissingBookingWrongOwnerAndInvalidAmount() {
        when(transactionRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(orderRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());
        assertThrows(PaymentException.class, () -> service.createOrder(7L, 99L, "a"));

        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(booking(10L, 8L, 100.0, null)));
        assertThrows(PaymentException.class, () -> service.createOrder(7L, 10L, "b"));
        when(orderRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(booking(11L, 7L, 0.0, null)));
        assertThrows(PaymentException.class, () -> service.createOrder(7L, 11L, "c"));
    }

    @Test
    void retryOrderShouldRejectPaidAndRefundedBookings() {
        when(orderRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(booking(10L, 7L, 100.0, PaymentStatus.SUCCESS)));
        assertThrows(PaymentException.class, () -> service.retryOrder(7L, 10L, "retry"));
        when(orderRepository.findByIdForUpdate(11L))
                .thenReturn(Optional.of(booking(11L, 7L, 100.0, PaymentStatus.REFUNDED)));
        assertThrows(PaymentException.class, () -> service.retryOrder(7L, 11L, "retry"));
    }

    @Test
    void verifyShouldReturnAlreadyPaidWithoutCallingGateway() {
        PaymentTransaction transaction = transaction(10L, 7L, PaymentTransactionStatus.PAID);
        when(transactionRepository.findByRazorpayOrderId("order_1")).thenReturn(Optional.of(transaction));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(booking(10L, 7L, 100.0, PaymentStatus.SUCCESS)));

        var response = service.verify(7L, new VerifyPaymentRequest("order_1", "pay_1", "sig"));

        assertTrue(response.verified());
        assertEquals("SUCCESS", response.paymentStatus());
        verifyNoInteractions(signatureVerifier);
    }

    @Test
    void verifyShouldMarkFailedWhenSignatureIsInvalid() {
        PaymentTransaction transaction = transaction(10L, 7L, PaymentTransactionStatus.CREATED);
        Order booking = booking(10L, 7L, 100.0, PaymentStatus.PENDING);
        when(transactionRepository.findByRazorpayOrderId("order_1")).thenReturn(Optional.of(transaction));
        when(signatureVerifier.verifyPayment("order_1", "pay_1", "bad", "key-secret")).thenReturn(false);
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(booking));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(booking));

        var response = service.verify(7L, new VerifyPaymentRequest("order_1", "pay_1", "bad"));

        assertFalse(response.verified());
        assertEquals("FAILED", response.paymentStatus());
        assertTrue(response.retryPaymentAllowed());
        assertEquals(PaymentStatus.FAILED, booking.getPaymentStatus());
        verify(eventPublisher).publishEvent(any(PaymentFailedEvent.class));
        verify(metricsService).incrementPaymentFailed();
    }

    @Test
    void verifyShouldMarkCapturedPaymentAndConfirmCreatedBooking() {
        PaymentTransaction transaction = transaction(10L, 7L, PaymentTransactionStatus.CREATED);
        Order booking = booking(10L, 7L, 100.0, PaymentStatus.PENDING);
        booking.setStatus(OrderStatus.CREATED);
        when(transactionRepository.findByRazorpayOrderId("order_1")).thenReturn(Optional.of(transaction));
        when(signatureVerifier.verifyPayment("order_1", "pay_1", "sig", "key-secret")).thenReturn(true);
        when(gateway.fetchPayment("pay_1"))
                .thenReturn(new RazorpayGateway.RazorpayPayment("pay_1", "order_1", 10000L, "captured"));
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(booking));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(booking));

        var response = service.verify(7L, new VerifyPaymentRequest("order_1", "pay_1", "sig"));

        assertTrue(response.verified());
        assertEquals(PaymentTransactionStatus.PAID, transaction.getPaymentStatus());
        assertEquals(PaymentStatus.SUCCESS, booking.getPaymentStatus());
        assertEquals(OrderStatus.CONFIRMED, booking.getStatus());
        verify(eventPublisher).publishEvent(any(PaymentCapturedEvent.class));
        verify(metricsService).incrementPaymentSuccess();
    }

    @Test
    void verifyShouldRejectGatewayOrderAndAmountMismatch() {
        PaymentTransaction transaction = transaction(10L, 7L, PaymentTransactionStatus.CREATED);
        when(transactionRepository.findByRazorpayOrderId("order_1")).thenReturn(Optional.of(transaction));
        when(signatureVerifier.verifyPayment(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        when(gateway.fetchPayment("wrong-order"))
                .thenReturn(new RazorpayGateway.RazorpayPayment("wrong-order", "order_2", 10000L, "captured"));
        assertThrows(PaymentException.class,
                () -> service.verify(7L, new VerifyPaymentRequest("order_1", "wrong-order", "sig")));

        when(gateway.fetchPayment("wrong-amount"))
                .thenReturn(new RazorpayGateway.RazorpayPayment("wrong-amount", "order_1", 9999L, "captured"));
        assertThrows(PaymentException.class,
                () -> service.verify(7L, new VerifyPaymentRequest("order_1", "wrong-amount", "sig")));
    }

    @Test
    void latestAndPayableChecksShouldEnforceOwnershipAndAmount() {
        Order booking = booking(10L, 7L, 100.0, PaymentStatus.PENDING);
        PaymentTransaction transaction = transaction(10L, 7L, PaymentTransactionStatus.CREATED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(transactionRepository.findTopByBookingIdOrderByCreatedAtDesc(10L)).thenReturn(Optional.of(transaction));

        assertEquals("PENDING", service.getLatest(7L, 10L).paymentStatus());
        assertDoesNotThrow(() -> service.assertPayableBooking(7L, 10L));
        assertThrows(PaymentException.class, () -> service.getLatest(8L, 10L));

        booking.setTotalAmount(0.0);
        assertThrows(PaymentException.class, () -> service.assertPayableBooking(7L, 10L));
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(PaymentException.class, () -> service.getLatest(7L, 99L));
    }

    @Test
    void processWebhookShouldValidateHeadersSignatureAndHandleDuplicate() {
        assertThrows(PaymentException.class, () -> service.processWebhook(" ", "sig", "{}"));
        when(signatureVerifier.verifyWebhook("{}", "bad", "webhook-secret")).thenReturn(false);
        assertThrows(PaymentException.class, () -> service.processWebhook("evt-1", "bad", "{}"));

        String body = "{\"event\":\"payment.authorized\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_1\",\"order_id\":\"order_1\"}}}}";
        PaymentWebhookEvent existing = new PaymentWebhookEvent();
        existing.setEventType("payment.authorized");
        existing.setStatus(PaymentWebhookEventStatus.SUCCESS);
        when(signatureVerifier.verifyWebhook(body, "sig", "webhook-secret")).thenReturn(true);
        when(webhookEventRepository.findByIdempotencyKeyForUpdate("event:evt-2"))
                .thenReturn(Optional.of(existing));

        assertTrue(service.processWebhook("evt-2", "sig", body).duplicate());
    }

    @Test
    void processWebhookShouldPersistUnsupportedEventAsSuccessful() {
        String body = "{\"event\":\"unknown.event\",\"payload\":{}}";
        when(signatureVerifier.verifyWebhook(body, "sig", "webhook-secret")).thenReturn(true);
        when(webhookEventRepository.findByIdempotencyKeyForUpdate("event:evt-3")).thenReturn(Optional.empty());
        MDC.put("requestId", "req-1");
        MDC.put("correlationId", "corr-1");

        var result = service.processWebhook("evt-3", "sig", body);

        assertTrue(result.processed());
        ArgumentCaptor<PaymentWebhookEvent> captor = ArgumentCaptor.forClass(PaymentWebhookEvent.class);
        verify(webhookEventRepository, atLeast(2)).save(captor.capture());
        assertEquals(PaymentWebhookEventStatus.SUCCESS,
                captor.getAllValues().get(captor.getAllValues().size() - 1).getStatus());
        assertEquals("req-1", captor.getAllValues().get(0).getRequestId());
        assertEquals("corr-1", captor.getAllValues().get(0).getCorrelationId());
        MDC.clear();
    }

    @Test
    void retryDueWebhookEventsShouldSkipTerminalAndRetryFailedPayload() {
        PaymentWebhookEvent terminal = webhook("terminal", PaymentWebhookEventStatus.SUCCESS, "{}");
        PaymentWebhookEvent failed = webhook("failed", PaymentWebhookEventStatus.FAILED, "not-json");
        when(webhookEventRepository.findRetryCandidates(anyList(), any(LocalDateTime.class)))
                .thenReturn(List.of(terminal, failed));
        when(webhookEventRepository.findByEventIdForUpdate("terminal")).thenReturn(Optional.of(terminal));
        when(webhookEventRepository.findByEventIdForUpdate("failed")).thenReturn(Optional.of(failed));

        assertThrows(PaymentException.class, service::retryDueWebhookEvents);
        verify(webhookEventRepository, never()).save(terminal);
        verify(webhookEventRepository).save(failed);
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

    private static PaymentWebhookEvent webhook(String eventId, PaymentWebhookEventStatus status, String payload) {
        PaymentWebhookEvent event = new PaymentWebhookEvent();
        event.setEventId(eventId);
        event.setEventType("unknown.event");
        event.setStatus(status);
        event.setRawPayload(payload);
        return event;
    }
}
