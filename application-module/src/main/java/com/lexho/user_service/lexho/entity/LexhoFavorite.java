package com.lexho.user_service.lexho.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "lexho_favorites", indexes = {
        @Index(name = "idx_lexho_fav_user", columnList = "userId"),
        @Index(name = "idx_lexho_fav_service", columnList = "serviceId")
})
public class LexhoFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long serviceId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }
}
