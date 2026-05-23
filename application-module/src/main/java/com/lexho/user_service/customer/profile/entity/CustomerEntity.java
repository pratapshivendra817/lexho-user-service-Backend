package com.lexho.user_service.customer.profile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "customers")
public class CustomerEntity {

    @Id
    private Long id;

    @Column(name = "full_name", length = 120)
    private String fullName;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "mobile", length = 20)
    private String mobile;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private CustomerGender gender;

    @Column(name = "profile_image_url", length = 512)
    private String profileImageUrl;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "profile_edit_locked")
    private Boolean profileEditLocked;

    @Column(name = "email_verified")
    private Boolean emailVerified;

    @Column(name = "phone_verified")
    private Boolean phoneVerified;

    @Column(name = "profile_image_approved")
    private Boolean profileImageApproved;

    @Column(name = "admin_notes", length = 1000)
    private String adminNotes;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (active == null) {
            active = true;
        }
        if (profileEditLocked == null) {
            profileEditLocked = false;
        }
        if (emailVerified == null) {
            emailVerified = false;
        }
        if (phoneVerified == null) {
            phoneVerified = false;
        }
        if (profileImageApproved == null) {
            profileImageApproved = true;
        }
        if ((mobile == null || mobile.isBlank()) && phoneNumber != null && !phoneNumber.isBlank()) {
            mobile = phoneNumber;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public CustomerGender getGender() {
        return gender;
    }

    public void setGender(CustomerGender gender) {
        this.gender = gender;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getProfileEditLocked() {
        return profileEditLocked;
    }

    public void setProfileEditLocked(Boolean profileEditLocked) {
        this.profileEditLocked = profileEditLocked;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public Boolean getPhoneVerified() {
        return phoneVerified;
    }

    public void setPhoneVerified(Boolean phoneVerified) {
        this.phoneVerified = phoneVerified;
    }

    public Boolean getProfileImageApproved() {
        return profileImageApproved;
    }

    public void setProfileImageApproved(Boolean profileImageApproved) {
        this.profileImageApproved = profileImageApproved;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }
}
