package com.lexho.user_service.dispatch.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "pincode_adjacency")
public class PincodeAdjacencyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_pincode", nullable = false, length = 20)
    private String sourcePincode;

    @Column(name = "adjacent_pincode", nullable = false, length = 20)
    private String adjacentPincode;

    @Column(nullable = false)
    private int priority;

    @Column(nullable = false)
    private boolean active = true;

    public Long getId() {
        return id;
    }

    public String getSourcePincode() {
        return sourcePincode;
    }

    public void setSourcePincode(String sourcePincode) {
        this.sourcePincode = sourcePincode;
    }

    public String getAdjacentPincode() {
        return adjacentPincode;
    }

    public void setAdjacentPincode(String adjacentPincode) {
        this.adjacentPincode = adjacentPincode;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

