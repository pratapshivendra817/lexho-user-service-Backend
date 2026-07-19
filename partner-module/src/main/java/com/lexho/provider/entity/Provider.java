package com.lexho.provider.entity;

import com.lexho.catalog.entity.ServiceCategory;
import com.lexho.provider.enums.KycStatus;
import com.lexho.provider.enums.OnboardingStatus;
import com.lexho.user.entity.User;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(
        name = "partners",
        indexes = {
                @Index(name = "idx_providers_onboarding_status", columnList = "onboarding_status"),
                @Index(name = "idx_providers_kyc_status", columnList = "kyc_status"),
                @Index(name = "idx_providers_city", columnList = "city"),
                @Index(name = "idx_providers_service_category", columnList = "service_category_id"),
                @Index(name = "idx_providers_phone_number", columnList = "phone_number")
        }
)
@Getter
@Setter
public class Provider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_code", unique = true, length = 20)
    private String providerCode;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "phone_number", nullable = false, unique = true, length = 20)
    private String phoneNumber;

    @Column(length = 20)
    private String gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "current_city", length = 100)
    private String currentCity;

    @Column(name = "current_state", length = 100)
    private String currentState;

    @Column(name = "current_full_address", length = 500)
    private String currentFullAddress;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(name = "full_address", length = 500)
    private String fullAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_category_id", nullable = false)
    private ServiceCategory serviceCategory;

    @Column(name = "experience_years", nullable = false)
    private Integer experienceYears;

    @ElementCollection
    @CollectionTable(name = "provider_service_areas", joinColumns = @JoinColumn(name = "provider_id"))
    @Column(name = "service_area", length = 100, nullable = false)
    private Set<String> serviceAreas = new HashSet<>();

    @Column(name = "account_number", nullable = false, length = 64)
    private String accountNumber;

    @Column(name = "ifsc_code", nullable = false, length = 20)
    private String ifscCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 20)
    private KycStatus kycStatus;

    @Column(name = "kyc_failed_reason", length = 500)
    private String kycFailedReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_status", nullable = false, length = 20)
    private OnboardingStatus onboardingStatus;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "blocked_reason", length = 500)
    private String blockedReason;

    @Column(name = "is_online", nullable = false)
    private boolean online;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "provider")
    private List<ProviderDocument> documents = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean deleted;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (kycStatus == null) {
            kycStatus = KycStatus.PENDING;
        }
        if (onboardingStatus == null) {
            onboardingStatus = OnboardingStatus.PENDING;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Set<String> getServiceAreas() {
        return serviceAreas == null ? Collections.emptySet() : new HashSet<>(serviceAreas);
    }

    public void setServiceAreas(Set<String> serviceAreas) {
        this.serviceAreas = serviceAreas == null ? new HashSet<>() : new HashSet<>(serviceAreas);
    }

    public List<ProviderDocument> getDocuments() {
        return documents == null ? Collections.emptyList() : new ArrayList<>(documents);
    }

    public void setDocuments(List<ProviderDocument> documents) {
        this.documents = documents == null ? new ArrayList<>() : new ArrayList<>(documents);
    }
}
