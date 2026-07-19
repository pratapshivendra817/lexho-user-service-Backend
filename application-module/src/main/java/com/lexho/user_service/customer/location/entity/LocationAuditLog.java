package com.lexho.user_service.customer.location.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "location_audit_logs")
public class LocationAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "customer_id", nullable = false)
    private Long customerId;
    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;
    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;
    @Column(length = 16)
    private String pincode;
    @Column(length = 32)
    private String source;
    @Column(name = "mock_location", nullable = false)
    private boolean mockLocation;
    @Column(name = "suspicious_jump", nullable = false)
    private boolean suspiciousJump;
    @Column(name = "risk_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal riskScore;
    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;
    @Column(name = "event_reason", length = 255)
    private String eventReason;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public void setSource(String source) { this.source = source; }
    public void setMockLocation(boolean mockLocation) { this.mockLocation = mockLocation; }
    public void setSuspiciousJump(boolean suspiciousJump) { this.suspiciousJump = suspiciousJump; }
    public void setRiskScore(BigDecimal riskScore) { this.riskScore = riskScore; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public void setEventReason(String eventReason) { this.eventReason = eventReason; }
}
