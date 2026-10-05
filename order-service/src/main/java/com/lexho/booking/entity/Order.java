package com.lexho.booking.entity;

import com.lexho.common.enums.OrderStatus;
import com.lexho.common.enums.PaymentStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "total_amount")
    private Double totalAmount;

    @Column(name = "original_amount")
    private BigDecimal originalAmount;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

    @Column(name = "coupon_code", length = 40)
    private String couponCode;

    @Convert(converter = OrderStatusConverter.class)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus;

    private String address;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "booking_type", length = 20)
    private String bookingType;

    @Column(name = "promised_start_time")
    private LocalDateTime promisedStartTime;

    @Column(name = "sla_minutes")
    private Integer slaMinutes;

    @Column(name = "unavailable_reason", length = 50)
    private String unavailableReason;

    @Column(name = "assigned_partner_id")
    private Long assignedPartnerId;

    @Column(name = "dispatch_address_id")
    private Long dispatchAddressId;

    @Column(name = "dispatch_service_id")
    private Long dispatchServiceId;

    @Column(name = "dispatch_quantity")
    private Integer dispatchQuantity;

    @Column(name = "dispatch_started_at")
    private LocalDateTime dispatchStartedAt;

    @Column(name = "arrived_at")
    private LocalDateTime arrivedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "in_progress_at")
    private LocalDateTime inProgressAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "service_start_pin_nonce", length = 64)
    private String serviceStartPinNonce;

    @Column(name = "service_start_pin_expires_at")
    private LocalDateTime serviceStartPinExpiresAt;

    @Column(name = "service_start_pin_attempts", nullable = false)
    private int serviceStartPinAttempts;

    @Column(name = "service_start_pin_consumed_at")
    private LocalDateTime serviceStartPinConsumedAt;

    @Column(name = "service_complete_pin_nonce", length = 64)
    private String serviceCompletePinNonce;

    @Column(name = "service_complete_pin_expires_at")
    private LocalDateTime serviceCompletePinExpiresAt;

    @Column(name = "service_complete_pin_attempts", nullable = false)
    private int serviceCompletePinAttempts;

    @Column(name = "service_complete_pin_consumed_at")
    private LocalDateTime serviceCompletePinConsumedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items = new ArrayList<>();

    public Double getOriginalAmount() {
        return originalAmount == null ? null : originalAmount.doubleValue();
    }

    public void setOriginalAmount(Double originalAmount) {
        this.originalAmount = moneyOrNull(originalAmount);
    }

    public Double getDiscountAmount() {
        return discountAmount == null ? null : discountAmount.doubleValue();
    }

    public void setDiscountAmount(Double discountAmount) {
        this.discountAmount = moneyOrNull(discountAmount);
    }

    private BigDecimal moneyOrNull(Double amount) {
        return amount == null ? null : BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
    }

    public Order(Long id,
                 Long userId,
                 Double totalAmount,
                 OrderStatus status,
                 PaymentStatus paymentStatus,
                 String address,
                 LocalDateTime scheduledAt,
                 String bookingType,
                 LocalDateTime promisedStartTime,
                 Integer slaMinutes,
                 String unavailableReason,
                 Long assignedPartnerId,
                 LocalDateTime arrivedAt,
                 LocalDateTime startedAt,
                 LocalDateTime inProgressAt,
                 LocalDateTime completedAt,
                 LocalDateTime createdAt,
                 LocalDateTime updatedAt,
                 List<OrderItem> items) {
        this.id = id;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.address = address;
        this.scheduledAt = scheduledAt;
        this.bookingType = bookingType;
        this.promisedStartTime = promisedStartTime;
        this.slaMinutes = slaMinutes;
        this.unavailableReason = unavailableReason;
        this.assignedPartnerId = assignedPartnerId;
        this.arrivedAt = arrivedAt;
        this.startedAt = startedAt;
        this.inProgressAt = inProgressAt;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.items = items;
    }

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
