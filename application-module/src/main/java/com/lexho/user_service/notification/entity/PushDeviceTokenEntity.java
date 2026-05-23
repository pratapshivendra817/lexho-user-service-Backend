package com.lexho.user_service.notification.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "push_device_tokens")
public class PushDeviceTokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 30)
    private String role;

    @Column(name = "device_id", nullable = false, length = 120)
    private String deviceId;

    @Column(nullable = false, length = 500)
    private String token;

    @Column(length = 30)
    private String platform;

    @Column(name = "app_type", length = 30)
    private String appType;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @PrePersist
    public void onCreate() {
        if (lastSeenAt == null) {
            lastSeenAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void onUpdate() {
        lastSeenAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getAppType() { return appType; }
    public void setAppType(String appType) { this.appType = appType; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
}
