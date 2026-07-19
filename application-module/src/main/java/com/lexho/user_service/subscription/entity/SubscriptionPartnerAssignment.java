package com.lexho.user_service.subscription.entity;

import com.lexho.user_service.subscription.enums.PartnerAssignmentStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_partner_assignment", indexes = {
        @Index(name = "idx_subscription_partner_assignment_subscription", columnList = "subscription_id"),
        @Index(name = "idx_subscription_partner_assignment_partner", columnList = "partner_id")
})
public class SubscriptionPartnerAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subscription_id", nullable = false, unique = true)
    private Long subscriptionId;

    @Column(name = "partner_id", nullable = false)
    private Long partnerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PartnerAssignmentStatus status;

    @Column(name = "replacement_partner_id")
    private Long replacementPartnerId;

    @Column(name = "replacement_reason", length = 500)
    private String replacementReason;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "replaced_at")
    private LocalDateTime replacedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.assignedAt == null) {
            this.assignedAt = now;
        }
        if (this.status == null) {
            this.status = PartnerAssignmentStatus.ASSIGNED;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(Long subscriptionId) { this.subscriptionId = subscriptionId; }
    public Long getPartnerId() { return partnerId; }
    public void setPartnerId(Long partnerId) { this.partnerId = partnerId; }
    public PartnerAssignmentStatus getStatus() { return status; }
    public void setStatus(PartnerAssignmentStatus status) { this.status = status; }
    public Long getReplacementPartnerId() { return replacementPartnerId; }
    public void setReplacementPartnerId(Long replacementPartnerId) { this.replacementPartnerId = replacementPartnerId; }
    public String getReplacementReason() { return replacementReason; }
    public void setReplacementReason(String replacementReason) { this.replacementReason = replacementReason; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
    public LocalDateTime getReplacedAt() { return replacedAt; }
    public void setReplacedAt(LocalDateTime replacedAt) { this.replacedAt = replacedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
