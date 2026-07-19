package com.lexho.user_service.lexhopartner.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "partner_settlement_refunds")
public class PartnerSettlementRefund {

    @Id
    @Column(name = "razorpay_refund_id", length = 100)
    private String razorpayRefundId;

    @Column(name = "payout_id", nullable = false)
    private Long payoutId;

    @Column(name = "reversed_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal reversedAmount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public String getRazorpayRefundId() {
        return razorpayRefundId;
    }

    public void setRazorpayRefundId(String razorpayRefundId) {
        this.razorpayRefundId = razorpayRefundId;
    }

    public Long getPayoutId() {
        return payoutId;
    }

    public void setPayoutId(Long payoutId) {
        this.payoutId = payoutId;
    }

    public BigDecimal getReversedAmount() {
        return reversedAmount;
    }

    public void setReversedAmount(BigDecimal reversedAmount) {
        this.reversedAmount = reversedAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
