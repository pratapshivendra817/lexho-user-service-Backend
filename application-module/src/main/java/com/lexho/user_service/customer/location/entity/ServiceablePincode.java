package com.lexho.user_service.customer.location.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "serviceable_pincodes")
public class ServiceablePincode {
    @Id
    @Column(nullable = false, length = 36, columnDefinition = "char(36)")
    private String id;

    @Column(nullable = false, length = 16, unique = true)
    private String pincode;
    @Column(nullable = false, length = 128)
    private String city;
    @Column(nullable = false)
    private boolean active;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
