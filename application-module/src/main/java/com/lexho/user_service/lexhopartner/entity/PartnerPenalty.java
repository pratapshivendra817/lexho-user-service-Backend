package com.lexho.user_service.lexhopartner.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "partner_penalties")
@Getter
@Setter
public class PartnerPenalty extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_id", nullable = false)
    private Long partnerId;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_by_admin_id")
    private Long createdByAdminId;

    @Column(name = "acknowledged_at")
    private java.time.LocalDateTime acknowledgedAt;

    @Column(name = "dispute_status", length = 30)
    private String disputeStatus;

    @Column(name = "dispute_reason", length = 1000)
    private String disputeReason;

    @Column(name = "disputed_at")
    private java.time.LocalDateTime disputedAt;

    @Column(name = "dispute_review_note", length = 1000)
    private String disputeReviewNote;

    @Column(name = "dispute_reviewed_by_admin_id")
    private Long disputeReviewedByAdminId;

    @Column(name = "dispute_reviewed_at")
    private java.time.LocalDateTime disputeReviewedAt;
}
