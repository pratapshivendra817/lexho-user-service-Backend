package com.lexho.user_service.admin.serviceablearea.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "serviceability_rules")
public class ServiceabilityRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, length = 120)
    private String city;
    @Column(nullable = false, length = 120)
    private String state;
    @Column(nullable = false, length = 20)
    private String pincode;
    @Column(name = "city_id")
    private Long cityId;
    @Column(name = "zone_id")
    private Long zoneId;
    @Lob
    @Column(name = "polygon_json", columnDefinition = "LONGTEXT")
    private String polygonJson;
    @Column(nullable = false)
    private boolean active = true;
    @Column(nullable = false)
    private boolean deleted = false;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getPincode() { return pincode; }
    public Long getCityId() { return cityId; }
    public Long getZoneId() { return zoneId; }
    public String getPolygonJson() { return polygonJson; }
    public boolean isActive() { return active; }
    public boolean isDeleted() { return deleted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setCity(String city) { this.city = city; }
    public void setState(String state) { this.state = state; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public void setCityId(Long cityId) { this.cityId = cityId; }
    public void setZoneId(Long zoneId) { this.zoneId = zoneId; }
    public void setPolygonJson(String polygonJson) { this.polygonJson = polygonJson; }
    public void setActive(boolean active) { this.active = active; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
