package com.lexho.user_service.accountdeletion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "account_deletion_requests")
public class AccountDeletionRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "user_type", nullable = false, length = 30)
    private String userType;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(name = "request_reason", length = 500)
    private String requestReason;
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
    @Column(name = "active_bookings", nullable = false)
    private long activeBookings;
    @Column(name = "pending_payments", nullable = false)
    private long pendingPayments;
    @Column(name = "pending_refunds", nullable = false)
    private long pendingRefunds;
    @Column(name = "pending_partner_payouts", nullable = false)
    private long pendingPartnerPayouts;
    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;
    @Column(name = "reviewed_by_admin_id")
    private Long reviewedByAdminId;
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    @Column(name = "processed_by_admin_id")
    private Long processedByAdminId;
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        requestedAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRequestReason() { return requestReason; }
    public void setRequestReason(String requestReason) { this.requestReason = requestReason; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public long getActiveBookings() { return activeBookings; }
    public void setActiveBookings(long activeBookings) { this.activeBookings = activeBookings; }
    public long getPendingPayments() { return pendingPayments; }
    public void setPendingPayments(long pendingPayments) { this.pendingPayments = pendingPayments; }
    public long getPendingRefunds() { return pendingRefunds; }
    public void setPendingRefunds(long pendingRefunds) { this.pendingRefunds = pendingRefunds; }
    public long getPendingPartnerPayouts() { return pendingPartnerPayouts; }
    public void setPendingPartnerPayouts(long pendingPartnerPayouts) { this.pendingPartnerPayouts = pendingPartnerPayouts; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public Long getReviewedByAdminId() { return reviewedByAdminId; }
    public void setReviewedByAdminId(Long reviewedByAdminId) { this.reviewedByAdminId = reviewedByAdminId; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public Long getProcessedByAdminId() { return processedByAdminId; }
    public void setProcessedByAdminId(Long processedByAdminId) { this.processedByAdminId = processedByAdminId; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
