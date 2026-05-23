package com.lexho.user_service.customer.location.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "customer_current_locations", indexes = {
        @Index(name = "idx_customer_current_locations_customer_id", columnList = "customer_id")
})
public class CustomerCurrentLocation {

    @Id
    @UuidGenerator
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
}
