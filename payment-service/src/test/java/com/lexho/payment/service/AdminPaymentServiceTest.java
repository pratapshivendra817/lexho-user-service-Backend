package com.lexho.payment.service;

import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.booking.service.OrderLifecycleService;
import com.lexho.common.enums.OrderStatus;
import com.lexho.common.enums.PaymentStatus;
import com.lexho.payment.dto.AdminRefundRequest;
import com.lexho.payment.entity.PaymentRefund;
import com.lexho.payment.entity.PaymentRefundStatus;
import com.lexho.payment.entity.PaymentTransaction;
import com.lexho.payment.entity.PaymentTransactionStatus;
import com.lexho.payment.exception.PaymentException;
import com.lexho.payment.event.PaymentCapturedEvent;
import com.lexho.payment.event.PaymentRefundProcessedEvent;
import com.lexho.payment.repository.PaymentRefundRepository;
import com.lexho.payment.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminPaymentServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock PaymentTransactionRepository transactionRepository;
    @Mock PaymentRefundRepository refundRepository;
    @Mock RazorpayGateway gateway;
    @Mock ApplicationEventPublisher eventPublisher;
    AdminPaymentService service;

    @BeforeEach
    void setUp() {
        service = new AdminPaymentService(orderRepository, transactionRepository, refundRepository, gateway,
                eventPublisher, new OrderLifecycleService(orderRepository), null);
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void dashboardShouldAggregatePaymentCounters() {
        when(transactionRepository.count()).thenReturn(10L);
        when(transactionRepository.countByPaymentStatus(PaymentTransactionStatus.CREATED)).thenReturn(2L);
        when(transactionRepository.countByPaymentStatus(PaymentTransactionStatus.PAID)).thenReturn(5L);
        when(transactionRepository.countByPaymentStatus(PaymentTransactionStatus.FAILED)).thenReturn(2L);
        when(transactionRepository.countByPaymentStatus(PaymentTransactionStatus.REFUNDED)).thenReturn(1L);
        when(transactionRepository.sumPaidAmount()).thenReturn(BigDecimal.valueOf(5000));
        when(refundRepository.sumProcessedAmount()).thenReturn(BigDecimal.valueOf(500));

        var response = service.dashboard();

        assertEquals(10L, response.totalAttempts());
        assertEquals(BigDecimal.valueOf(5000), response.paidAmount());
        assertEquals(BigDecimal.valueOf(500), response.processedRefundAmount());
    }

    @Test
    void transactionsShouldParseStatusClampPagingAndRejectInvalidStatus() {
        PaymentTransaction transaction = transaction(1L, PaymentTransactionStatus.PAID);
        when(transactionRepository.search(eq(10L), eq(PaymentTransactionStatus.PAID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(transaction)));

        var response = service.transactions(10L, " paid ", -1, 500);

        assertEquals(1, response.items().size());
        assertEquals("PAID", response.items().get(0).paymentStatus());
        assertThrows(PaymentException.class, () -> service.transactions(null, "unknown", 0, 10));
    }

    @Test
    void reconciliationShouldMapTransactionsAndOnlyProcessedRefunds() {
        Order booking = booking(10L);
        PaymentRefund processed = refund(1L, PaymentRefundStatus.PROCESSED, "25.00");
        PaymentRefund failed = refund(2L, PaymentRefundStatus.FAILED, "10.00");
        when(orderRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(refundRepository.findByBookingIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(processed, failed));
        when(transactionRepository.findByBookingIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(transaction(1L, PaymentTransactionStatus.PAID)));

        var response = service.reconciliation(10L);

        assertEquals(new BigDecimal("25.00"), response.processedRefundAmount());
        assertEquals(1, response.transactions().size());
        assertEquals(2, response.refunds().size());
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(PaymentException.class, () -> service.reconciliation(99L));
    }

    @Test
    void createRefundShouldReturnExistingIdempotentRefund() {
        PaymentRefund existing = refund(1L, PaymentRefundStatus.PROCESSED, "25.00");
        when(refundRepository.findByIdempotencyKey("same")).thenReturn(Optional.of(existing));

        assertEquals("PROCESSED", service.createRefund(7L, 10L, "same",
                new AdminRefundRequest(new BigDecimal("25.00"), "reason")).refundStatus());
        assertThrows(PaymentException.class, () -> service.createRefund(7L, 10L, " ",
                new AdminRefundRequest(BigDecimal.ONE, "reason")));
        verifyNoInteractions(transactionRepository, gateway);
    }

    @Test
    void createRefundShouldRejectMissingPaymentIdAndExcessAmount() {
        PaymentTransaction transaction = transaction(1L, PaymentTransactionStatus.PAID);
        transaction.setRazorpayPaymentId(null);
        when(refundRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(transactionRepository.findTopByBookingIdAndPaymentStatusInOrderByCreatedAtDesc(eq(10L), anyList()))
                .thenReturn(Optional.of(transaction));
        when(transactionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(transaction));
        assertThrows(PaymentException.class, () -> service.createRefund(7L, 10L, "a",
                new AdminRefundRequest(BigDecimal.TEN, "reason")));

        transaction.setRazorpayPaymentId("pay_1");
        when(refundRepository.sumAmountByTransactionIdAndStatuses(eq(1L), anyList()))
                .thenReturn(new BigDecimal("90.00"));
        assertThrows(PaymentException.class, () -> service.createRefund(7L, 10L, "b",
                new AdminRefundRequest(new BigDecimal("20.00"), "reason")));
    }

    @Test
    void createRefundShouldPersistProcessedRefundAndFullyRefundBooking() {
        PaymentTransaction transaction = transaction(1L, PaymentTransactionStatus.PAID);
        Order booking = booking(10L);
        when(refundRepository.findByIdempotencyKey("refund-1")).thenReturn(Optional.empty());
        when(transactionRepository.findTopByBookingIdAndPaymentStatusInOrderByCreatedAtDesc(eq(10L), anyList()))
                .thenReturn(Optional.of(transaction));
        when(transactionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(transaction));
        when(refundRepository.sumAmountByTransactionIdAndStatuses(eq(1L), anyList()))
                .thenReturn(BigDecimal.ZERO, new BigDecimal("100.00"));
        when(gateway.createRefund("pay_1", new BigDecimal("100.00"), 10L))
                .thenReturn(new RazorpayGateway.RazorpayRefund("refund_1", "pay_1", 10000L, "processed"));
        when(refundRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(booking));

        var response = service.createRefund(7L, 10L, "refund-1",
                new AdminRefundRequest(new BigDecimal("100"), "  duplicate charge  "));

        assertEquals("PROCESSED", response.refundStatus());
        assertEquals("duplicate charge", response.reason());
        assertEquals(PaymentTransactionStatus.REFUNDED, transaction.getPaymentStatus());
        assertEquals(PaymentStatus.REFUNDED, booking.getPaymentStatus());
        verify(eventPublisher).publishEvent(any(PaymentRefundProcessedEvent.class));
    }

    @Test
    void createRefundShouldReturnFailedWhenGatewayFails() {
        PaymentTransaction transaction = transaction(1L, PaymentTransactionStatus.PAID);
        when(refundRepository.findByIdempotencyKey("refund-2")).thenReturn(Optional.empty());
        when(transactionRepository.findTopByBookingIdAndPaymentStatusInOrderByCreatedAtDesc(eq(10L), anyList()))
                .thenReturn(Optional.of(transaction));
        when(transactionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(transaction));
        when(refundRepository.sumAmountByTransactionIdAndStatuses(eq(1L), anyList())).thenReturn(BigDecimal.ZERO);
        when(refundRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(gateway.createRefund(anyString(), any(), anyLong())).thenThrow(new PaymentException("gateway down"));

        var response = service.createRefund(7L, 10L, "refund-2",
                new AdminRefundRequest(BigDecimal.TEN, "reason"));

        assertEquals("FAILED", response.refundStatus());
        assertEquals("gateway down", response.failureReason());
    }

    @Test
    void reconcileLatestShouldCaptureAndConfirmBooking() {
        PaymentTransaction transaction = transaction(1L, PaymentTransactionStatus.CREATED);
        Order booking = booking(10L);
        booking.setStatus(OrderStatus.CREATED);
        when(transactionRepository.findTopByBookingIdOrderByCreatedAtDesc(10L)).thenReturn(Optional.of(transaction));
        when(gateway.fetchPayment("pay_1"))
                .thenReturn(new RazorpayGateway.RazorpayPayment("pay_1", "order_1", 10000L, "captured"));
        when(gateway.toPaise(new BigDecimal("100.00"))).thenReturn(10000L);
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(booking));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(refundRepository.findByBookingIdOrderByCreatedAtDesc(10L)).thenReturn(List.of());
        when(transactionRepository.findByBookingIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(transaction));

        var response = service.reconcileLatest(10L);

        assertEquals("PAID", response.transactions().get(0).paymentStatus());
        assertEquals(PaymentStatus.SUCCESS, booking.getPaymentStatus());
        assertEquals(OrderStatus.CONFIRMED, booking.getStatus());
        verify(eventPublisher).publishEvent(any(PaymentCapturedEvent.class));
    }

    @Test
    void reconcileLatestShouldBeIdempotentAndNotMoveTerminalBookingsBackwards() {
        PaymentTransaction paidTransaction = transaction(1L, PaymentTransactionStatus.PAID);
        Order confirmedBooking = booking(10L);
        confirmedBooking.setStatus(OrderStatus.CONFIRMED);
        confirmedBooking.setPaymentStatus(PaymentStatus.SUCCESS);
        when(transactionRepository.findTopByBookingIdOrderByCreatedAtDesc(10L)).thenReturn(Optional.of(paidTransaction));
        when(gateway.fetchPayment("pay_1"))
                .thenReturn(new RazorpayGateway.RazorpayPayment("pay_1", "order_1", 10000L, "captured"));
        when(gateway.toPaise(new BigDecimal("100.00"))).thenReturn(10000L);
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(confirmedBooking));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(confirmedBooking));
        when(refundRepository.findByBookingIdOrderByCreatedAtDesc(10L)).thenReturn(List.of());
        when(transactionRepository.findByBookingIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(paidTransaction));

        var response = service.reconcileLatest(10L);

        assertEquals("PAID", response.transactions().get(0).paymentStatus());
        assertEquals(OrderStatus.CONFIRMED, confirmedBooking.getStatus());
        verify(eventPublisher, never()).publishEvent(any(PaymentCapturedEvent.class));

        PaymentTransaction cancelledTransaction = transaction(2L, PaymentTransactionStatus.CREATED);
        Order cancelledBooking = booking(11L);
        cancelledBooking.setStatus(OrderStatus.CANCELLED);
        cancelledTransaction.setBookingId(11L);
        when(transactionRepository.findTopByBookingIdOrderByCreatedAtDesc(11L)).thenReturn(Optional.of(cancelledTransaction));
        when(orderRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(cancelledBooking));
        when(orderRepository.findById(11L)).thenReturn(Optional.of(cancelledBooking));
        when(refundRepository.findByBookingIdOrderByCreatedAtDesc(11L)).thenReturn(List.of());
        when(transactionRepository.findByBookingIdOrderByCreatedAtDesc(11L)).thenReturn(List.of(cancelledTransaction));

        service.reconcileLatest(11L);

        assertEquals(OrderStatus.CANCELLED, cancelledBooking.getStatus());
    }

    private static PaymentTransaction transaction(Long id, PaymentTransactionStatus status) {
        PaymentTransaction transaction = new PaymentTransaction();
        ReflectionTestUtils.setField(transaction, "id", id);
        transaction.setBookingId(10L);
        transaction.setCustomerId(7L);
        transaction.setRazorpayOrderId("order_1");
        transaction.setRazorpayPaymentId("pay_1");
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setCurrency("INR");
        transaction.setPaymentStatus(status);
        return transaction;
    }

    private static PaymentRefund refund(Long id, PaymentRefundStatus status, String amount) {
        PaymentRefund refund = new PaymentRefund();
        ReflectionTestUtils.setField(refund, "id", id);
        refund.setBookingId(10L);
        refund.setAmount(new BigDecimal(amount));
        refund.setCurrency("INR");
        refund.setReason("reason");
        refund.setRefundStatus(status);
        return refund;
    }

    private static Order booking(Long id) {
        Order booking = new Order();
        booking.setId(id);
        booking.setUserId(7L);
        booking.setTotalAmount(100.0);
        booking.setStatus(OrderStatus.CREATED);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        return booking;
    }
}
