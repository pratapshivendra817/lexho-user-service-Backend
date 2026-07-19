package com.lexho.payment.entity;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentEntitiesTest {

    @Test
    void paymentTransactionGettersSettersAndLifecycleMethodsWork() throws Exception {
        PaymentTransaction transaction = new PaymentTransaction();
        LocalDateTime paidAt = LocalDateTime.of(2026, 6, 19, 11, 15);

        setField(transaction, "id", 1L);
        transaction.setBookingId(10L);
        transaction.setCustomerId(7L);
        transaction.setIdempotencyKey("idem-1");
        transaction.setRazorpayOrderId("order_1");
        transaction.setRazorpayPaymentId("pay_1");
        transaction.setRazorpaySignature("sig_1");
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setCurrency("INR");
        transaction.setPaymentStatus(PaymentTransactionStatus.PAID);
        transaction.setFailureReason("failed once");
        transaction.setPaidAt(paidAt);

        transaction.onCreate();
        LocalDateTime createdAt = transaction.getCreatedAt();
        LocalDateTime updatedAt = transaction.getUpdatedAt();
        transaction.onUpdate();

        assertEquals(1L, getField(transaction, "id"));
        assertEquals(10L, transaction.getBookingId());
        assertEquals(7L, transaction.getCustomerId());
        assertEquals("idem-1", transaction.getIdempotencyKey());
        assertEquals("order_1", transaction.getRazorpayOrderId());
        assertEquals("pay_1", transaction.getRazorpayPaymentId());
        assertEquals("sig_1", transaction.getRazorpaySignature());
        assertEquals(new BigDecimal("100.00"), transaction.getAmount());
        assertEquals("INR", transaction.getCurrency());
        assertEquals(PaymentTransactionStatus.PAID, transaction.getPaymentStatus());
        assertEquals("failed once", transaction.getFailureReason());
        assertEquals(paidAt, transaction.getPaidAt());
        assertNotNull(createdAt);
        assertNotNull(updatedAt);
        assertTrue(transaction.getUpdatedAt().isAfter(updatedAt) || transaction.getUpdatedAt().isEqual(updatedAt));
        assertNotEquals(System.identityHashCode(new PaymentTransaction()), System.identityHashCode(transaction));
        assertTrue(transaction.toString().contains("PaymentTransaction"));
    }

    @Test
    void paymentRefundGettersSettersAndLifecycleMethodsWork() throws Exception {
        PaymentRefund refund = new PaymentRefund();
        LocalDateTime processedAt = LocalDateTime.of(2026, 6, 19, 12, 30);

        setField(refund, "id", 2L);
        refund.setPaymentTransactionId(10L);
        refund.setBookingId(20L);
        refund.setRequestedByAdminId(99L);
        refund.setIdempotencyKey("refund-1");
        refund.setRazorpayRefundId("rf_1");
        refund.setAmount(new BigDecimal("25.00"));
        refund.setCurrency("INR");
        refund.setRefundStatus(PaymentRefundStatus.PROCESSED);
        refund.setReason("duplicate payment");
        refund.setFailureReason("none");
        refund.setProcessedAt(processedAt);

        refund.onCreate();
        LocalDateTime updatedAt = refund.getUpdatedAt();
        refund.onUpdate();

        assertEquals(2L, getField(refund, "id"));
        assertEquals(10L, refund.getPaymentTransactionId());
        assertEquals(20L, refund.getBookingId());
        assertEquals(99L, refund.getRequestedByAdminId());
        assertEquals("refund-1", refund.getIdempotencyKey());
        assertEquals("rf_1", refund.getRazorpayRefundId());
        assertEquals(new BigDecimal("25.00"), refund.getAmount());
        assertEquals("INR", refund.getCurrency());
        assertEquals(PaymentRefundStatus.PROCESSED, refund.getRefundStatus());
        assertEquals("duplicate payment", refund.getReason());
        assertEquals("none", refund.getFailureReason());
        assertEquals(processedAt, refund.getProcessedAt());
        assertNotNull(refund.getCreatedAt());
        assertTrue(refund.getUpdatedAt().isAfter(updatedAt) || refund.getUpdatedAt().isEqual(updatedAt));
        assertTrue(refund.toString().contains("PaymentRefund"));
    }

    @Test
    void paymentWebhookEventDefaultsAndLifecycleMethodsWork() throws Exception {
        PaymentWebhookEvent event = new PaymentWebhookEvent();

        setField(event, "id", 3L);
        event.setEventId("evt_1");
        event.setEventType("payment.captured");
        event.setIdempotencyKey("event:evt_1");
        event.setRazorpayPaymentId("pay_1");
        event.setBookingId(42L);
        event.setRetryCount(2);
        event.setNextRetryAt(LocalDateTime.of(2026, 6, 19, 13, 0));
        event.setRawPayload("{\"event\":\"payment.captured\"}");
        event.setFailureReason("temporary");

        event.onCreate();
        LocalDateTime receivedAt = event.getReceivedAt();
        LocalDateTime updatedAt = (LocalDateTime) getField(event, "updatedAt");
        event.onUpdate();

        assertEquals(3L, getField(event, "id"));
        assertEquals("evt_1", event.getEventId());
        assertEquals("payment.captured", event.getEventType());
        assertEquals("event:evt_1", event.getIdempotencyKey());
        assertEquals("pay_1", event.getRazorpayPaymentId());
        assertEquals(42L, event.getBookingId());
        assertEquals(PaymentWebhookEventStatus.PENDING, event.getStatus());
        assertNotNull(receivedAt);
        assertEquals(2, event.getRetryCount());
        assertNotNull(event.getNextRetryAt());
        assertEquals("{\"event\":\"payment.captured\"}", event.getRawPayload());
        assertEquals("temporary", event.getFailureReason());
        LocalDateTime currentUpdatedAt = (LocalDateTime) getField(event, "updatedAt");
        assertTrue(currentUpdatedAt.isAfter(updatedAt) || currentUpdatedAt.isEqual(updatedAt));
        assertTrue(event.toString().contains("PaymentWebhookEvent"));
    }

    @Test
    void paymentWebhookEventPreservesExplicitStatusAndProcessedAt() {
        PaymentWebhookEvent event = new PaymentWebhookEvent();
        LocalDateTime processedAt = LocalDateTime.of(2026, 6, 19, 14, 0);

        event.setStatus(PaymentWebhookEventStatus.SUCCESS);
        event.setProcessedAt(processedAt);
        event.onCreate();

        assertEquals(PaymentWebhookEventStatus.SUCCESS, event.getStatus());
        assertEquals(processedAt, event.getProcessedAt());
    }

    @Test
    void enumValuesExposeExpectedOrdering() {
        assertEquals(4, PaymentTransactionStatus.values().length);
        assertEquals(PaymentTransactionStatus.CREATED, PaymentTransactionStatus.valueOf("CREATED"));
        assertEquals(0, PaymentTransactionStatus.CREATED.ordinal());
        assertEquals(1, PaymentRefundStatus.PROCESSED.ordinal());
        assertEquals(PaymentWebhookEventStatus.DEAD_LETTER, PaymentWebhookEventStatus.valueOf("DEAD_LETTER"));
        assertFalse(PaymentRefundStatus.FAILED.name().isBlank());
        assertNull(new PaymentTransaction().getFailureReason());
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object getField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}
