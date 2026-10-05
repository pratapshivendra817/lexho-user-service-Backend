package com.lexho.payment.repository;

import com.lexho.payment.entity.PaymentTransaction;
import com.lexho.payment.entity.PaymentRefund;
import com.lexho.payment.entity.PaymentRefundStatus;
import com.lexho.payment.entity.PaymentTransactionStatus;
import com.lexho.payment.entity.PaymentWebhookEvent;
import com.lexho.payment.entity.PaymentWebhookEventStatus;
import com.lexho.common.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class PaymentPersistenceIntegrityTest {

    @Autowired
    private PaymentTransactionRepository transactionRepository;

    @Autowired
    private PaymentWebhookEventRepository webhookEventRepository;

    @Autowired
    private PaymentRefundRepository refundRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private RollbackHarness rollbackHarness;

    @Test
    void paymentTransactionRejectsDuplicateIdempotencyKeyAtDatabaseLevel() {
        transactionRepository.saveAndFlush(transaction("idem-1", "order-1"));

        assertThatThrownBy(() -> transactionRepository.saveAndFlush(transaction("idem-1", "order-2")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void webhookSearchTreatsSqlMetaCharactersAsLiteralInput() {
        webhookEventRepository.saveAndFlush(webhook("evt-1", "payment.captured", "payment.captured:pay-1", "pay-1"));
        webhookEventRepository.saveAndFlush(webhook("evt-2", "payment.failed", "payment.failed:pay-2", "pay-2"));

        var result = webhookEventRepository.search("' OR 1=1 --", null, null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void paymentRefundRejectsDuplicateIdempotencyKeyAtDatabaseLevel() {
        refundRepository.saveAndFlush(refund("refund-idem-1", "rfnd-1"));

        assertThatThrownBy(() -> refundRepository.saveAndFlush(refund("refund-idem-1", "rfnd-2")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentDuplicateIdempotencyKeyAllowsOnlyOneCommittedTransaction() throws Exception {
        transactionRepository.deleteAll();
        String sharedKey = "race-" + UUID.randomUUID();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Throwable>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < 2; i++) {
                final int index = i;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                                transactionRepository.saveAndFlush(transaction(sharedKey, "order-race-" + index)));
                        return null;
                    } catch (Throwable throwable) {
                        return throwable;
                    }
                }));
            }

            ready.await();
            start.countDown();

            int successCount = 0;
            int failureCount = 0;
            for (Future<Throwable> future : futures) {
                Throwable throwable = future.get();
                if (throwable == null) {
                    successCount++;
                } else {
                    failureCount++;
                    assertThat(throwable).isInstanceOfAny(DataIntegrityViolationException.class, RuntimeException.class);
                }
            }

            assertThat(successCount).isEqualTo(1);
            assertThat(failureCount).isEqualTo(1);
            assertThat(transactionRepository.findByIdempotencyKey(sharedKey)).isPresent();
            assertThat(transactionRepository.count()).isEqualTo(1L);
        } finally {
            executor.shutdownNow();
            transactionRepository.deleteAll();
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentDuplicateRefundIdempotencyAllowsOnlyOneCommittedRefund() throws Exception {
        refundRepository.deleteAll();
        String sharedKey = "refund-race-" + UUID.randomUUID();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Throwable>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < 2; i++) {
                final int index = i;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                                refundRepository.saveAndFlush(refund(sharedKey, "rfnd-race-" + index)));
                        return null;
                    } catch (Throwable throwable) {
                        return throwable;
                    }
                }));
            }

            ready.await();
            start.countDown();

            int successCount = 0;
            int failureCount = 0;
            for (Future<Throwable> future : futures) {
                Throwable throwable = future.get();
                if (throwable == null) {
                    successCount++;
                } else {
                    failureCount++;
                    assertThat(throwable).isInstanceOfAny(DataIntegrityViolationException.class, RuntimeException.class);
                }
            }

            assertThat(successCount).isEqualTo(1);
            assertThat(failureCount).isEqualTo(1);
            assertThat(refundRepository.findByIdempotencyKey(sharedKey)).isPresent();
            assertThat(refundRepository.count()).isEqualTo(1L);
        } finally {
            executor.shutdownNow();
            refundRepository.deleteAll();
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void paymentConfirmationRollsBackTransactionAndBookingTogether() {
        TestBooking booking = persistBooking(710L);
        PaymentTransaction transaction = transactionRepository.saveAndFlush(transaction("confirm-rollback", "order-confirm-rollback", booking.getId()));

        assertThatThrownBy(() -> rollbackHarness.confirmPaymentThenFail(transaction.getId(), booking.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("payment confirmation rollback");

        assertThat(transactionRepository.findById(transaction.getId()).orElseThrow().getPaymentStatus())
                .isEqualTo(PaymentTransactionStatus.CREATED);
        assertThat(entityManager.find(TestBooking.class, booking.getId()).getPaymentStatus())
                .isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void refundProcessingRollsBackRefundTransactionAndBookingTogether() {
        TestBooking booking = persistBooking(711L);
        PaymentTransaction transaction = transactionRepository.saveAndFlush(transaction("refund-rollback-base", "order-refund-rollback", booking.getId()));

        assertThatThrownBy(() -> rollbackHarness.processRefundThenFail(transaction.getId(), booking.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("refund rollback");

        assertThat(refundRepository.findByIdempotencyKey("refund-rollback-" + booking.getId())).isEmpty();
        assertThat(transactionRepository.findById(transaction.getId()).orElseThrow().getPaymentStatus())
                .isEqualTo(PaymentTransactionStatus.CREATED);
        assertThat(entityManager.find(TestBooking.class, booking.getId()).getPaymentStatus())
                .isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void checkedExceptionRollbackIsExplicitlyCoveredForPaymentWrites() {
        assertThatThrownBy(() -> rollbackHarness.createPaymentArtifactsThenCheckedFail(712L))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("checked rollback");

        assertThat(transactionRepository.findByIdempotencyKey("checked-rollback-712")).isEmpty();
        assertThat(refundRepository.findByIdempotencyKey("checked-refund-rollback-712")).isEmpty();
    }

    private static PaymentTransaction transaction(String idempotencyKey, String orderId) {
        return transaction(idempotencyKey, orderId, 10L);
    }

    private static PaymentTransaction transaction(String idempotencyKey, String orderId, Long bookingId) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setBookingId(bookingId);
        transaction.setCustomerId(7L);
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setRazorpayOrderId(orderId);
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setCurrency("INR");
        transaction.setPaymentStatus(PaymentTransactionStatus.CREATED);
        return transaction;
    }

    private static TestBooking booking(Long customerId) {
        TestBooking order = new TestBooking();
        order.setUserId(customerId);
        order.setTotalAmount(100.0);
        order.setStatus(com.lexho.common.enums.OrderStatus.CREATED);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setAddress("Rollback address");
        return order;
    }

    private TestBooking persistBooking(Long customerId) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            TestBooking booking = booking(customerId);
            entityManager.persist(booking);
            entityManager.flush();
            entityManager.clear();
            return booking;
        });
    }

    private static PaymentWebhookEvent webhook(String eventId, String eventType, String idempotencyKey, String paymentId) {
        PaymentWebhookEvent event = new PaymentWebhookEvent();
        event.setEventId(eventId);
        event.setEventType(eventType);
        event.setIdempotencyKey(idempotencyKey);
        event.setRazorpayPaymentId(paymentId);
        event.setStatus(PaymentWebhookEventStatus.SUCCESS);
        event.setRawPayload("{\"event\":\"" + eventType + "\"}");
        return event;
    }

    private static PaymentRefund refund(String idempotencyKey, String refundId) {
        PaymentRefund refund = new PaymentRefund();
        refund.setPaymentTransactionId(10L);
        refund.setBookingId(20L);
        refund.setRequestedByAdminId(1L);
        refund.setIdempotencyKey(idempotencyKey);
        refund.setRazorpayRefundId(refundId);
        refund.setAmount(new BigDecimal("25.00"));
        refund.setCurrency("INR");
        refund.setRefundStatus(PaymentRefundStatus.REQUESTED);
        refund.setReason("Test refund");
        return refund;
    }

    static class RollbackHarness {
        private final PaymentTransactionRepository transactionRepository;
        private final PaymentRefundRepository refundRepository;
        private final EntityManager entityManager;

        RollbackHarness(PaymentTransactionRepository transactionRepository,
                        PaymentRefundRepository refundRepository,
                        EntityManager entityManager) {
            this.transactionRepository = transactionRepository;
            this.refundRepository = refundRepository;
            this.entityManager = entityManager;
        }

        @Transactional
        void confirmPaymentThenFail(Long transactionId, Long bookingId) {
            PaymentTransaction transaction = transactionRepository.findByIdForUpdate(transactionId).orElseThrow();
            transaction.setPaymentStatus(PaymentTransactionStatus.PAID);
            transaction.setRazorpayPaymentId("pay-confirm-rollback");
            transactionRepository.saveAndFlush(transaction);

            TestBooking booking = entityManager.find(TestBooking.class, bookingId);
            booking.setPaymentStatus(PaymentStatus.SUCCESS);
            booking.setStatus(com.lexho.common.enums.OrderStatus.CONFIRMED);
            entityManager.flush();

            throw new IllegalStateException("payment confirmation rollback");
        }

        @Transactional
        void processRefundThenFail(Long transactionId, Long bookingId) {
            PaymentTransaction transaction = transactionRepository.findByIdForUpdate(transactionId).orElseThrow();
            PaymentRefund refund = refund("refund-rollback-" + bookingId, "rfnd-rollback-" + bookingId);
            refund.setPaymentTransactionId(transactionId);
            refund.setBookingId(bookingId);
            refund.setRefundStatus(PaymentRefundStatus.PROCESSED);
            refundRepository.saveAndFlush(refund);

            transaction.setPaymentStatus(PaymentTransactionStatus.REFUNDED);
            transactionRepository.saveAndFlush(transaction);

            TestBooking booking = entityManager.find(TestBooking.class, bookingId);
            booking.setPaymentStatus(PaymentStatus.REFUNDED);
            entityManager.flush();

            throw new IllegalStateException("refund rollback");
        }

        @Transactional(rollbackFor = Exception.class)
        void createPaymentArtifactsThenCheckedFail(Long bookingId) throws Exception {
            transactionRepository.saveAndFlush(transaction("checked-rollback-" + bookingId, "order-checked-" + bookingId, bookingId));
            PaymentRefund refund = refund("checked-refund-rollback-" + bookingId, "rfnd-checked-" + bookingId);
            refund.setBookingId(bookingId);
            refundRepository.saveAndFlush(refund);
            throw new Exception("checked rollback");
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
            PaymentTransaction.class,
            PaymentWebhookEvent.class,
            com.lexho.payment.entity.PaymentRefund.class,
            TestBooking.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            PaymentTransactionRepository.class,
            PaymentWebhookEventRepository.class,
            com.lexho.payment.repository.PaymentRefundRepository.class
    })
    @Import(RollbackHarness.class)
    static class TestConfiguration {
    }

    @Entity(name = "PaymentRollbackBooking")
    @Table(name = "orders")
    static class TestBooking {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private Long userId;
        private Double totalAmount;

        @Enumerated(EnumType.STRING)
        private com.lexho.common.enums.OrderStatus status;

        @Enumerated(EnumType.STRING)
        private PaymentStatus paymentStatus;

        private String address;

        @Column(name = "created_at", nullable = false, updatable = false)
        private java.time.LocalDateTime createdAt;

        @Column(name = "updated_at", nullable = false)
        private java.time.LocalDateTime updatedAt;

        @PrePersist
        void onCreate() {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            createdAt = now;
            updatedAt = now;
        }

        Long getId() {
            return id;
        }

        void setUserId(Long userId) {
            this.userId = userId;
        }

        void setTotalAmount(Double totalAmount) {
            this.totalAmount = totalAmount;
        }

        void setStatus(com.lexho.common.enums.OrderStatus status) {
            this.status = status;
        }

        void setPaymentStatus(PaymentStatus paymentStatus) {
            this.paymentStatus = paymentStatus;
        }

        PaymentStatus getPaymentStatus() {
            return paymentStatus;
        }

        void setAddress(String address) {
            this.address = address;
        }
    }
}
