package com.lexho.user_service.customer.location.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "customer_addresses", indexes = {
        @Index(name = "idx_customer_addresses_customer_id", columnList = "customer_id"),
        @Index(name = "idx_customer_addresses_pincode", columnList = "pincode"),
        @Index(name = "idx_customer_addresses_is_default", columnList = "is_default")
})
public class CustomerAddress extends BaseEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AddressType type;

    @Column(name = "custom_label", length = 64)
    private String customLabel;
    @Column(name = "flat_no", length = 32)
    private String flatNo;
    @Column(length = 32)
    private String floor;
    @Column(name = "building_name", length = 128)
    private String buildingName;
    @Column(length = 128)
    private String landmark;
    @Column(name = "address_line", nullable = false, length = 255)
    private String addressLine;
    @Column(length = 128)
    private String area;
    @Column(length = 128)
    private String sector;
    @Column(nullable = false, length = 128)
    private String city;
    @Column(nullable = false, length = 128)
    private String state;
    @Column(nullable = false, length = 128)
    private String country;
    @Column(nullable = false, length = 16)
    private String pincode;
    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;
    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;
    @Column(length = 20)
    private String mobile;
    @Column(name = "is_default", nullable = false)
    private boolean isDefault;
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @PrePersist
    public void defaults() {
        if (country == null || country.isBlank()) {
            country = "India";
        }
    }

    public UUID getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public AddressType getType() { return type; }
    public void setType(AddressType type) { this.type = type; }
    public String getCustomLabel() { return customLabel; }
    public void setCustomLabel(String customLabel) { this.customLabel = customLabel; }
    public String getFlatNo() { return flatNo; }
    public void setFlatNo(String flatNo) { this.flatNo = flatNo; }
    public String getFloor() { return floor; }
    public void setFloor(String floor) { this.floor = floor; }
    public String getBuildingName() { return buildingName; }
    public void setBuildingName(String buildingName) { this.buildingName = buildingName; }
    public String getLandmark() { return landmark; }
    public void setLandmark(String landmark) { this.landmark = landmark; }
    public String getAddressLine() { return addressLine; }
    public void setAddressLine(String addressLine) { this.addressLine = addressLine; }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }
    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }
}
