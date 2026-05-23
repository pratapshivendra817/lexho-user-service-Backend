package com.lexho.user_service.admin.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "service_details", indexes = {
        @Index(name = "idx_service_details_service", columnList = "service_id"),
        @Index(name = "idx_service_details_type", columnList = "type")
})
public class ServiceDetailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private AdminServiceEntity service;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DetailType type;

    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public AdminServiceEntity getService() {
        return service;
    }

    public void setService(AdminServiceEntity service) {
        this.service = service;
    }

    public DetailType getType() {
        return type;
    }

    public void setType(DetailType type) {
        this.type = type;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
}
