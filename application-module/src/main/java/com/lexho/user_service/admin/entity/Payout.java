package com.lexho.user_service.admin.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "payouts", uniqueConstraints = {
        @UniqueConstraint(name = "uq_payouts_reference_booking", columnNames = "reference_booking_id")
})
public class Payout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(nullable = false)
    private Double amount;

    @Column(name = "gross_amount", precision = 12, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "commission_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionPercent = BigDecimal.ZERO;

    @Column(name = "commission_amount", precision = 12, scale = 2)
    private BigDecimal commissionAmount;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(name = "reversed_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal reversedAmount = BigDecimal.ZERO;

    @Column(name = "reversed_at")
    private LocalDateTime reversedAt;

    @Column(nullable = false, length = 40)
    private String status;

    @Column(name = "reference_booking_id")
    private Long referenceBookingId;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getProviderId() {
        return providerId;
    }

    public void setProviderId(Long providerId) {
        this.providerId = providerId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public void setGrossAmount(BigDecimal grossAmount) {
        this.grossAmount = grossAmount;
    }

    public BigDecimal getCommissionPercent() {
        return commissionPercent;
    }

    public void setCommissionPercent(BigDecimal commissionPercent) {
        this.commissionPercent = commissionPercent;
    }

    public BigDecimal getCommissionAmount() {
        return commissionAmount;
    }

    public void setCommissionAmount(BigDecimal commissionAmount) {
        this.commissionAmount = commissionAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getReversedAmount() {
        return reversedAmount;
    }

    public void setReversedAmount(BigDecimal reversedAmount) {
        this.reversedAmount = reversedAmount;
    }

    public LocalDateTime getReversedAt() {
        return reversedAt;
    }

    public void setReversedAt(LocalDateTime reversedAt) {
        this.reversedAt = reversedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getReferenceBookingId() {
        return referenceBookingId;
    }

    public void setReferenceBookingId(Long referenceBookingId) {
        this.referenceBookingId = referenceBookingId;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
