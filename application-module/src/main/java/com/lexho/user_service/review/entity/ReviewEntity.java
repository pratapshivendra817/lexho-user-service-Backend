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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity(name = "MarketplaceReview")
@Table(
        name = "reviews",
        indexes = {
                @Index(name = "idx_reviews_booking_id", columnList = "booking_id"),
                @Index(name = "idx_reviews_customer_id", columnList = "customer_id"),
                @Index(name = "idx_reviews_provider_id", columnList = "provider_id"),
                @Index(name = "idx_reviews_service_id", columnList = "service_id"),
                @Index(name = "idx_reviews_status", columnList = "status"),
                @Index(name = "idx_reviews_report_count", columnList = "report_count")
        }
)
@Getter
@Setter
public class ReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "provider_id")
    private Long providerId;

    @Column(name = "service_id")
    private Long serviceId;

    @Column(nullable = false)
    private Integer rating;

    @Column(name = "review_comment", length = 1000)
    private String reviewComment;

    @Column(name = "review_images", columnDefinition = "LONGTEXT")
    private String reviewImages;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewStatus status;

    @Column(name = "report_count", nullable = false)
    private Long reportCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean deleted;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = ReviewStatus.PENDING;
        }
        if (reportCount == null) {
            reportCount = 0L;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        if (reportCount == null) {
            reportCount = 0L;
        }
    }
}
