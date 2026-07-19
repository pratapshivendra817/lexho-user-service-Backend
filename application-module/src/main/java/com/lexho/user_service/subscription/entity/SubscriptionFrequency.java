package com.lexho.user_service.subscription.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "subscription_frequency", indexes = {
        @Index(name = "idx_subscription_frequency_subscription", columnList = "subscription_id")
})
public class SubscriptionFrequency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subscription_id", nullable = false)
    private Long subscriptionId;

    @Column(name = "day_name", nullable = false, length = 20)
    private String dayName;

    public Long getId() { return id; }
    public Long getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(Long subscriptionId) { this.subscriptionId = subscriptionId; }
    public String getDayName() { return dayName; }
    public void setDayName(String dayName) { this.dayName = dayName; }
}
