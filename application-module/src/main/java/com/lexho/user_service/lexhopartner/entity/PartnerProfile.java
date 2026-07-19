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
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "partners", indexes = {
        @Index(name = "idx_partners_mobile", columnList = "mobile_number", unique = true),
        @Index(name = "idx_partners_email", columnList = "email"),
        @Index(name = "idx_partners_city", columnList = "city")
})
@Getter
@Setter
public class PartnerProfile extends BaseEntity {

    @Id
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "mobile_number", nullable = false, length = 20)
    private String mobileNumber;

    @Column(length = 120)
    private String email;

    @Column(length = 100)
    private String city;

    @Column(length = 20)
    private String pincode;

    @Column(length = 255)
    private String address;

    @Column(name = "service_category", length = 80)
    private String serviceCategory;

    @Column(nullable = false)
    private Double rating = 0.0;

    @Column(name = "total_reviews", nullable = false)
    private Long totalReviews = 0L;

    @Column(name = "profile_image", length = 1000)
    private String profileImage;

    @Column(name = "is_online", nullable = false)
    private boolean online;

    @Column(name = "is_verified", nullable = false)
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
}
