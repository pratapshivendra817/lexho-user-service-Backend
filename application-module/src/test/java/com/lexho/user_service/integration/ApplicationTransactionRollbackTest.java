package com.lexho.user_service.integration;

import com.lexho.common.enums.PaymentStatus;
import com.lexho.user_service.lexhopartner.entity.OrderStatus;
import com.lexho.user_service.lexhopartner.entity.OrderStatusHistory;
import com.lexho.user_service.lexhopartner.entity.PartnerOrder;
import com.lexho.user_service.lexhopartner.entity.PaymentMode;
import com.lexho.testconfig.LexhoMySqlDataJpaTest;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@LexhoMySqlDataJpaTest
@ContextConfiguration(classes = ApplicationTransactionRollbackTest.JpaConfig.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ApplicationTransactionRollbackTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private RollbackHarness rollbackHarness;

    @Test
    void bookingCreationRollsBackOrderAndItemsTogether() {
        assertThatThrownBy(() -> rollbackHarness.createBookingThenFail(501L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("booking rollback");

        assertThat(countOrdersByUserId(501L)).isZero();
    }

    @Test
    void payoutCreationRollsBackWhenSettlementFails() {
        assertThatThrownBy(() -> rollbackHarness.createPayoutThenFail(701L, 801L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("payout rollback");

        assertThat(countPayoutsByReferenceBookingId(801L)).isZero();
    }

    @Test
    void providerAssignmentRollsBackOrderAndHistoryTogether() {
        PartnerOrder order = persistPartnerOrder();

        assertThatThrownBy(() -> rollbackHarness.assignProviderThenFail(order.getId(), 901L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("assignment rollback");

        PartnerOrder reloaded = entityManager.find(PartnerOrder.class, order.getId());
        assertThat(reloaded.getAssignedPartnerId()).isNull();
        assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(countHistoryByOrderId(order.getId())).isZero();
    }

    private long countOrdersByUserId(Long userId) {
        return entityManager.createQuery("select count(o) from AppRollbackBooking o where o.userId = :userId", Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
    }

    private long countPayoutsByReferenceBookingId(Long bookingId) {
        return entityManager.createQuery("select count(p) from AppRollbackPayout p where p.referenceBookingId = :bookingId", Long.class)
                .setParameter("bookingId", bookingId)
                .getSingleResult();
    }

    private long countHistoryByOrderId(Long orderId) {
        return entityManager.createQuery("select count(h) from OrderStatusHistory h where h.order.id = :orderId", Long.class)
                .setParameter("orderId", orderId)
                .getSingleResult();
    }

    private PartnerOrder persistPartnerOrder() {
        return new TransactionTemplate(transactionManager).execute(status -> {
            PartnerOrder order = partnerOrder();
            entityManager.persist(order);
            entityManager.flush();
            entityManager.clear();
            return order;
        });
    }

    private static PartnerOrder partnerOrder() {
        PartnerOrder order = new PartnerOrder();
        order.setCustomerName("Rollback Customer");
        order.setCustomerPhone("9999999999");
        order.setAddress("Noida");
        order.setLatitude(28.61);
        order.setLongitude(77.20);
        order.setServiceType("Cleaning");
        order.setAmount(250.0);
        order.setPaymentMode(PaymentMode.ONLINE);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setStatus(OrderStatus.CREATED);
        return order;
    }

    static class RollbackHarness {
        private final EntityManager entityManager;

        RollbackHarness(EntityManager entityManager) {
            this.entityManager = entityManager;
        }

        @Transactional
        void createBookingThenFail(Long customerId) {
            TestBooking order = new TestBooking();
            order.setUserId(customerId);
            order.setStatus(com.lexho.common.enums.OrderStatus.CONFIRMED);
            order.setPaymentStatus(PaymentStatus.PENDING);
            order.setTotalAmount(399.0);
            order.setAmount(399.0);
            order.setLatitude(28.61);
            order.setLongitude(77.20);
            order.setCustomerName("Rollback Customer");
            order.setCustomerPhone("9999999999");
            order.setServiceType("Cleaning");
            order.setPaymentMode(PaymentMode.ONLINE);
            order.setAddress("Rollback address");

            TestOrderItem item = new TestOrderItem();
            item.setPartnerId(11L);
            item.setPartnerName("Rollback Service");
            item.setPrice(399.0);
            item.setQuantity(1);
            item.setOrder(order);
            order.setItems(new ArrayList<>(List.of(item)));

            entityManager.persist(order);
            entityManager.flush();
            throw new IllegalStateException("booking rollback");
        }

        @Transactional
        void createPayoutThenFail(Long providerId, Long bookingId) {
            TestPayout payout = new TestPayout();
            payout.setProviderId(providerId);
            payout.setReferenceBookingId(bookingId);
            payout.setAmount(120.0);
            payout.setStatus("PENDING");
            entityManager.persist(payout);
            entityManager.flush();
            throw new IllegalStateException("payout rollback");
        }

        @Transactional
        void assignProviderThenFail(Long orderId, Long providerId) {
            PartnerOrder order = entityManager.find(PartnerOrder.class, orderId);
            OrderStatus from = order.getStatus();
            order.setAssignedPartnerId(providerId);
            order.setStatus(OrderStatus.ACCEPTED);
            entityManager.flush();

            OrderStatusHistory history = new OrderStatusHistory();
            history.setOrder(order);
            history.setFromStatus(from);
            history.setToStatus(OrderStatus.ACCEPTED);
            history.setActorType("PARTNER");
            history.setActorId(providerId);
            history.setRemarks("accepted");
            entityManager.persist(history);
            entityManager.flush();

            throw new IllegalStateException("assignment rollback");
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
            TestBooking.class,
            TestOrderItem.class,
            TestPayout.class,
            PartnerOrder.class,
            OrderStatusHistory.class
    })
    @Import(RollbackHarness.class)
    static class JpaConfig {
    }

    @Entity(name = "AppRollbackBooking")
    @Table(name = "orders")
    static class TestBooking {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "user_id")
        private Long userId;

        @Column(name = "total_amount")
        private Double totalAmount;

        private Double amount;
        private Double latitude;
        private Double longitude;

        @Column(name = "customer_name")
        private String customerName;

        @Column(name = "customer_phone")
        private String customerPhone;

        @Column(name = "service_type")
        private String serviceType;

        @Enumerated(EnumType.STRING)
        private com.lexho.common.enums.OrderStatus status;

        @Enumerated(EnumType.STRING)
        @Column(name = "payment_status")
        private PaymentStatus paymentStatus;

        @Enumerated(EnumType.STRING)
        @Column(name = "payment_mode")
        private PaymentMode paymentMode;

        private String address;

        @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
        private List<TestOrderItem> items = new ArrayList<>();

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

        void setUserId(Long userId) {
            this.userId = userId;
        }

        void setTotalAmount(Double totalAmount) {
            this.totalAmount = totalAmount;
        }

        void setAmount(Double amount) {
            this.amount = amount;
        }

        void setLatitude(Double latitude) {
            this.latitude = latitude;
        }

        void setLongitude(Double longitude) {
            this.longitude = longitude;
        }

        void setCustomerName(String customerName) {
            this.customerName = customerName;
        }

        void setCustomerPhone(String customerPhone) {
            this.customerPhone = customerPhone;
        }

        void setServiceType(String serviceType) {
            this.serviceType = serviceType;
        }

        void setStatus(com.lexho.common.enums.OrderStatus status) {
            this.status = status;
        }

        void setPaymentStatus(PaymentStatus paymentStatus) {
            this.paymentStatus = paymentStatus;
        }

        void setAddress(String address) {
            this.address = address;
        }

        void setPaymentMode(PaymentMode paymentMode) {
            this.paymentMode = paymentMode;
        }

        void setItems(List<TestOrderItem> items) {
            this.items = items;
        }
    }

    @Entity
    @Table(name = "order_items")
    static class TestOrderItem {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "partner_id")
        private Long partnerId;

        @Column(name = "partner_name")
        private String partnerName;
        private Double price;
        private Integer quantity;

        @ManyToOne
        @JoinColumn(name = "order_id")
        private TestBooking order;

        void setPartnerId(Long partnerId) {
            this.partnerId = partnerId;
        }

        void setPartnerName(String partnerName) {
            this.partnerName = partnerName;
        }

        void setPrice(Double price) {
            this.price = price;
        }

        void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        void setOrder(TestBooking order) {
            this.order = order;
        }
    }

    @Entity(name = "AppRollbackPayout")
    @Table(name = "payouts")
    static class TestPayout {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "provider_id", nullable = false)
        private Long providerId;

        @Column(nullable = false)
        private Double amount;

        @Column(nullable = false, length = 40)
        private String status;

        @Column(name = "reference_booking_id")
        private Long referenceBookingId;

        @Column(name = "created_at", nullable = false, updatable = false)
        private java.time.LocalDateTime createdAt;

        @PrePersist
        void onCreate() {
            createdAt = java.time.LocalDateTime.now();
        }

        void setProviderId(Long providerId) {
            this.providerId = providerId;
        }

        void setAmount(Double amount) {
            this.amount = amount;
        }

        void setStatus(String status) {
            this.status = status;
        }

        void setReferenceBookingId(Long referenceBookingId) {
            this.referenceBookingId = referenceBookingId;
        }
    }
}
