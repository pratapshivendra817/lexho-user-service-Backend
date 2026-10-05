package com.lexho.user_service.lexhopartner.entity;

import com.lexho.user_service.lexhopartner.model.PartnerOperationalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "partners", indexes = {
        @Index(name = "idx_partners_mobile", columnList = "phone_number", unique = true)
})
@Getter
@Setter
public class PartnerOperationalProfile extends BaseEntity {
    private static final String STATUS_PENDING = "PENDING";

    @Id
    private Long id;

    @Transient
    private String name;

    @Transient
    private String mobileNumber;

    @Transient
    private String serviceCategory;

    @Transient
    private Double rating = 0.0;

    @Column(name = "total_reviews", nullable = false)
    private Long totalReviews = 0L;

    @Column(name = "is_online", nullable = false)
    private boolean online;

    @Transient
    private boolean verified;

    // Legacy required columns in existing partners schema
    @Column(name = "account_number", nullable = false, length = 64)
    private String accountNumber = "";

    @Column(name = "ifsc_code", nullable = false, length = 20)
    private String ifscCode = "";

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName = "";

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber = "";

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "address_line1", length = 500)
    private String addressLine1;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "pincode", length = 20)
    private String pincode;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "experience_years", nullable = false)
    private int experienceYears = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_status", nullable = false, columnDefinition = "enum('APPROVED','BLOCKED','PENDING','REJECTED')")
    private PartnerOperationalStatus partnerOperationalStatus = PartnerOperationalStatus.PENDING;

    @Column(name = "service_category_id")
    private Long serviceCategoryId;

    @Column(name = "availability_locked_until")
    private LocalDateTime availabilityLockedUntil;

    @Column(name = "availability_lock_reason", length = 500)
    private String availabilityLockReason;

    @Column(name = "risk_status", length = 30)
    private String riskStatus = "NORMAL";

    @Column(name = "fraud_flag", nullable = false)
    private boolean fraudFlag = false;

    @Column(name = "admin_notes", length = 1000)
    private String adminNotes;

    @Column(name = "bank_verification_status", nullable = false, length = 30)
    private String bankVerificationStatus = STATUS_PENDING;

    @Column(name = "bank_rejection_reason", length = 500)
    private String bankRejectionReason;

    @Column(name = "document_status", nullable = false, length = 30)
    private String documentStatus = STATUS_PENDING;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "reviewed_by_admin_id")
    private Long reviewedByAdminId;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "aadhaar_front_url", length = 1000)
    private String aadhaarFrontUrl;

    @Column(name = "aadhaar_back_url", length = 1000)
    private String aadhaarBackUrl;

    @Column(name = "pan_card_url", length = 1000)
    private String panCardUrl;

    @Column(name = "selfie_url", length = 1000)
    private String selfieUrl;

    @Column(name = "certificate_url", length = 1000)
    private String certificateUrl;

    @ElementCollection
    @CollectionTable(name = "provider_service_types", joinColumns = @JoinColumn(name = "provider_id"))
    @Column(name = "service_type", length = 100)
    private Set<String> skills = new LinkedHashSet<>();

    public String getName() {
        return (name == null || name.isBlank()) ? fullName : name;
    }

    public void setName(String name) {
        this.name = name;
        this.fullName = name == null ? "" : name;
    }

    public String getMobileNumber() {
        return (mobileNumber == null || mobileNumber.isBlank()) ? phoneNumber : mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
        this.phoneNumber = mobileNumber == null ? "" : mobileNumber;
    }

    public String getServiceCategory() {
        return serviceCategory;
    }

    public void setServiceCategory(String serviceCategory) {
        this.serviceCategory = serviceCategory;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public boolean isVerified() {
        return verified || partnerOperationalStatus == PartnerOperationalStatus.APPROVED;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
        if (verified) {
            this.partnerOperationalStatus = PartnerOperationalStatus.APPROVED;
        } else if (this.partnerOperationalStatus == PartnerOperationalStatus.APPROVED) {
            this.partnerOperationalStatus = PartnerOperationalStatus.PENDING;
        }
    }
}
