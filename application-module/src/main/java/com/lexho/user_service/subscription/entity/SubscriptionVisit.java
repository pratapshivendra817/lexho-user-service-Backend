package com.lexho.user_service.subscription.entity;

import com.lexho.user_service.subscription.enums.VisitStatus;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_visit", indexes = {
        @Index(name = "idx_subscription_visit_subscription", columnList = "subscription_id"),
        @Index(name = "idx_subscription_visit_date", columnList = "subscription_id,visit_date")
})
public class SubscriptionVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subscription_id", nullable = false)
    private Long subscriptionId;

    @Column(name = "booking_id", unique = true)
    private Long bookingId;

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @Column(name = "time_slot", nullable = false, length = 50)
    private String timeSlot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VisitStatus status;

    @Column(name = "rescheduled_date")
    private LocalDate rescheduledDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = VisitStatus.SCHEDULED;
        }
    }

    public Long getId() { return id; }
    public Long getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(Long subscriptionId) { this.subscriptionId = subscriptionId; }
    public LocalDate getVisitDate() { return visitDate; }
    public void setVisitDate(LocalDate visitDate) { this.visitDate = visitDate; }
    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }
    public VisitStatus getStatus() { return status; }
    public void setStatus(VisitStatus status) { this.status = status; }
    public LocalDate getRescheduledDate() { return rescheduledDate; }
    public void setRescheduledDate(LocalDate rescheduledDate) { this.rescheduledDate = rescheduledDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
