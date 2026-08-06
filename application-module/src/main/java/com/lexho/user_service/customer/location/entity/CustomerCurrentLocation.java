package com.lexho.user_service.customer.location.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "customer_current_locations", indexes = {
        @Index(name = "idx_customer_current_locations_customer_id", columnList = "customer_id")
})
public class CustomerCurrentLocation {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 36, columnDefinition = "char(36)")
    private UUID id;

    @Column(name = "customer_id", nullable = false, unique = true)
    private Long customerId;
    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;
    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;
    @Column(name = "formatted_address", length = 255)
    private String formattedAddress;
    @Column(length = 128)
    private String city;
    @Column(length = 128)
    private String state;
    @Column(length = 16)
    private String pincode;
    @Column(name = "service_available", nullable = false)
    private boolean serviceAvailable;
    @Column(name = "manual_confirmed", nullable = false)
    private boolean manualConfirmed;
    @Column(name = "accuracy_meters", precision = 8, scale = 2)
    private BigDecimal accuracyMeters;
    @Column(name = "confidence_score", precision = 5, scale = 4)
    private BigDecimal confidenceScore;
    @Column(name = "location_age_seconds")
    private Integer locationAgeSeconds;
    @Column(name = "detection_source", length = 32)
    private String detectionSource;
    @Column(name = "mock_location", nullable = false)
    private boolean mockLocation;
    @Column(name = "suspicious_jump", nullable = false)
    private boolean suspiciousJump;
    @Column(name = "spoof_attempt_count", nullable = false)
    private Integer spoofAttemptCount;
    @Column(name = "risk_score", precision = 5, scale = 2, nullable = false)
    private BigDecimal riskScore;
    @Column(name = "last_idempotency_key", length = 128)
    private String lastIdempotencyKey;
    @Column(name = "last_location_fingerprint", length = 128)
    private String lastLocationFingerprint;

    public UUID getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public String getFormattedAddress() { return formattedAddress; }
    public void setFormattedAddress(String formattedAddress) { this.formattedAddress = formattedAddress; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public boolean isServiceAvailable() { return serviceAvailable; }
    public void setServiceAvailable(boolean serviceAvailable) { this.serviceAvailable = serviceAvailable; }
    public boolean isManualConfirmed() { return manualConfirmed; }
    public void setManualConfirmed(boolean manualConfirmed) { this.manualConfirmed = manualConfirmed; }
    public BigDecimal getAccuracyMeters() { return accuracyMeters; }
    public void setAccuracyMeters(BigDecimal accuracyMeters) { this.accuracyMeters = accuracyMeters; }
    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }
    public Integer getLocationAgeSeconds() { return locationAgeSeconds; }
    public void setLocationAgeSeconds(Integer locationAgeSeconds) { this.locationAgeSeconds = locationAgeSeconds; }
    public String getDetectionSource() { return detectionSource; }
    public void setDetectionSource(String detectionSource) { this.detectionSource = detectionSource; }
    public boolean isMockLocation() { return mockLocation; }
    public void setMockLocation(boolean mockLocation) { this.mockLocation = mockLocation; }
    public boolean isSuspiciousJump() { return suspiciousJump; }
    public void setSuspiciousJump(boolean suspiciousJump) { this.suspiciousJump = suspiciousJump; }
    public Integer getSpoofAttemptCount() { return spoofAttemptCount; }
    public void setSpoofAttemptCount(Integer spoofAttemptCount) { this.spoofAttemptCount = spoofAttemptCount; }
    public BigDecimal getRiskScore() { return riskScore; }
    public void setRiskScore(BigDecimal riskScore) { this.riskScore = riskScore; }
    public String getLastIdempotencyKey() { return lastIdempotencyKey; }
    public void setLastIdempotencyKey(String lastIdempotencyKey) { this.lastIdempotencyKey = lastIdempotencyKey; }
    public String getLastLocationFingerprint() { return lastLocationFingerprint; }
    public void setLastLocationFingerprint(String lastLocationFingerprint) { this.lastLocationFingerprint = lastLocationFingerprint; }
}
