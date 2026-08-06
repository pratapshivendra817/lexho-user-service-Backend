package com.lexho.user_service.lexhopartner.entity;

import com.lexho.provider.enums.KycStatus;
import com.lexho.provider.enums.OnboardingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "partners", indexes = {
        @Index(name = "idx_partners_mobile", columnList = "phone_number", unique = true),
        @Index(name = "idx_partners_email", columnList = "email"),
        @Index(name = "idx_partners_city", columnList = "city")
})
@Getter
@Setter
public class PartnerProfile extends BaseEntity {

    @Id
    private Long id;

    @Transient
    private String name;

    @Transient
    private String mobileNumber;

    @Column(length = 120)
    private String email;

    @Column(length = 100)
    private String city;

    @Column(length = 20)
    private String pincode;

    @Column(name = "full_address", length = 500)
    private String address;

    @Transient
    private String serviceCategory;

    @Transient
    private Double rating = 0.0;

    @Column(name = "total_reviews", nullable = false)
    private Long totalReviews = 0L;

    @Transient
    private String profileImage;

    @Transient
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

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "experience_years", nullable = false)
    private int experienceYears = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, columnDefinition = "enum('FAILED','PENDING','VERIFIED')")
    private KycStatus kycStatus = KycStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_status", nullable = false, columnDefinition = "enum('APPROVED','BLOCKED','PENDING','REJECTED')")
    private OnboardingStatus onboardingStatus = OnboardingStatus.PENDING;

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

    @Column(name = "kyc_workflow_status", nullable = false, length = 30)
    private String kycWorkflowStatus = "PENDING";

    @Column(name = "kyc_rejection_reason", length = 500)
    private String kycRejectionReason;

    @Column(name = "bank_verification_status", nullable = false, length = 30)
    private String bankVerificationStatus = "PENDING";

    @Column(name = "bank_rejection_reason", length = 500)
    private String bankRejectionReason;

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

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public boolean isVerified() {
        return verified || kycStatus == KycStatus.VERIFIED || "APPROVED".equalsIgnoreCase(kycWorkflowStatus);
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
        if (verified) {
            this.kycStatus = KycStatus.VERIFIED;
            this.kycWorkflowStatus = "APPROVED";
        } else if (this.kycStatus == KycStatus.VERIFIED) {
            this.kycStatus = KycStatus.PENDING;
            if ("APPROVED".equalsIgnoreCase(this.kycWorkflowStatus)) {
                this.kycWorkflowStatus = "PENDING";
            }
        }
    }
}
