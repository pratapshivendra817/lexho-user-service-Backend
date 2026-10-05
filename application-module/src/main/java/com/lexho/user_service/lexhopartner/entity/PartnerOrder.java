package com.lexho.user_service.lexhopartner.entity;

import com.lexho.common.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_status", columnList = "status"),
        @Index(name = "idx_orders_assigned_partner_id", columnList = "assigned_partner_id"),
        @Index(name = "idx_orders_created_at", columnList = "created_at")
})
@Getter
@Setter
public class PartnerOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_name", nullable = false, length = 120)
    private String customerName;

    @Column(name = "user_id")
    private Long customerId;

    @Column(name = "customer_phone", nullable = false, length = 20)
    private String customerPhone;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "service_type", nullable = false, length = 80)
    private String serviceType;

    @Column(name = "booking_type", length = 20)
    private String bookingType;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false, length = 20)
    private PaymentMode paymentMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus;

    @Convert(converter = PartnerOrderStatusConverter.class)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.CREATED;

    @Column(name = "assigned_partner_id")
    private Long assignedPartnerId;

    @Column(name = "before_image_urls", columnDefinition = "TINYTEXT")
    private String beforeImageUrls;

    @Column(name = "after_image_urls", columnDefinition = "TINYTEXT")
    private String afterImageUrls;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "arrived_at")
    private LocalDateTime arrivedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "in_progress_at")
    private LocalDateTime inProgressAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

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
}
