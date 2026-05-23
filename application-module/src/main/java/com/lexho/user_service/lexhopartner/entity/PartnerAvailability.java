package com.lexho.user_service.lexhopartner.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "partner_availability")
@Getter
@Setter
public class PartnerAvailability extends BaseEntity {

    @Id
    @Column(name = "partner_id")
    private Long partnerId;

    @Column(name = "is_online", nullable = false)
    private boolean online;
}
