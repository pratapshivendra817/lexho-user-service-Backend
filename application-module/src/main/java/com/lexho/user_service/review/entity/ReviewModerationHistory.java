package com.lexho.user_service.review.entity;

import com.lexho.user_service.review.enums.ReviewStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "review_moderation_history",
        indexes = {
                @Index(name = "idx_review_moderation_history_review", columnList = "review_id"),
                @Index(name = "idx_review_moderation_history_admin", columnList = "admin_id"),
                @Index(name = "idx_review_moderation_history_created", columnList = "created_at")
        }
)
@Getter
@Setter
public class ReviewModerationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false, length = 20)
    private ReviewStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private ReviewStatus newStatus;

    @Column(nullable = false, length = 40)
    private String action;

    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "admin_name", length = 180)
    private String adminName;

    @Column(name = "admin_email", length = 180)
    private String adminEmail;

    @Column(length = 1000)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
