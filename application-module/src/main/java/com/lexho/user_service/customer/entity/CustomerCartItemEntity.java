package com.lexho.user_service.customer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cart_items", uniqueConstraints = {
        @UniqueConstraint(name = "uk_cart_items_user_service", columnNames = {"user_id", "service_id"})
}, indexes = {
        @Index(name = "idx_cart_items_user_id", columnList = "user_id"),
        @Index(name = "idx_cart_items_service_id", columnList = "service_id")
})
@Getter
@Setter
public class CustomerCartItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    @Column(nullable = false)
    private Integer quantity;
}
