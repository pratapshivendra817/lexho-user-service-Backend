package com.lexho.user_service.lexhopartner.service;

import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.lexhopartner.dto.PartnerDocumentDto;
import com.lexho.user_service.lexhopartner.dto.PartnerProfileDto;
import com.lexho.user_service.lexhopartner.dto.PartnerAvailabilityResponse;
import com.lexho.user_service.lexhopartner.dto.UpdateAvailabilityRequest;
import com.lexho.user_service.lexhopartner.dto.UpdatePartnerProfileRequest;
import com.lexho.user_service.lexhopartner.entity.DocumentType;
import com.lexho.catalog.entity.ServiceCategory;
import com.lexho.partner.enums.KycStatus;
import com.lexho.partner.enums.OnboardingStatus;
import com.lexho.user_service.lexhopartner.entity.PartnerAvailability;
import com.lexho.user_service.lexhopartner.entity.PartnerDocument;
import com.lexho.user_service.lexhopartner.entity.PartnerProfile;
import com.lexho.user_service.lexhopartner.repository.PartnerAvailabilityRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerDocumentRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerProfileRepository;
import com.lexho.user_service.review.repository.ServiceCategoryLookupRepository;
import com.lexho.user_service.lexhopartner.util.AuditLogUtil;
import com.lexho.user_service.lexhopartner.util.FileStorageUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class PartnerProfileService {

    private final PartnerProfileRepository partnerProfileRepository;
    private final PartnerDocumentRepository partnerDocumentRepository;
    private final PartnerAvailabilityRepository partnerAvailabilityRepository;
    private final ServiceCategoryLookupRepository serviceCategoryLookupRepository;
    private final FileStorageUtil fileStorageUtil;
    private final AuditLogUtil auditLogUtil;

    public PartnerProfileService(PartnerProfileRepository partnerProfileRepository,
                                 PartnerDocumentRepository partnerDocumentRepository,
                                 PartnerAvailabilityRepository partnerAvailabilityRepository,
                                 ServiceCategoryLookupRepository serviceCategoryLookupRepository,
                                 FileStorageUtil fileStorageUtil,
                                 AuditLogUtil auditLogUtil) {
        this.partnerProfileRepository = partnerProfileRepository;
        this.partnerDocumentRepository = partnerDocumentRepository;
        this.partnerAvailabilityRepository = partnerAvailabilityRepository;
        this.serviceCategoryLookupRepository = serviceCategoryLookupRepository;
        this.fileStorageUtil = fileStorageUtil;
        this.auditLogUtil = auditLogUtil;
    }

    @Transactional
    public PartnerProfileDto getProfile(Long partnerId) {
        return toDto(getOrCreateProfile(partnerId));
    }

    @Transactional
    public PartnerProfileDto updateProfile(Long partnerId, UpdatePartnerProfileRequest request) {
        PartnerProfile profile = getOrCreateProfile(partnerId);
        profile.setName(request.name());
        profile.setEmail(request.email());
        profile.setCity(request.city());
        profile.setAddress(request.address());
        profile.setServiceCategory(request.serviceCategory());
        partnerProfileRepository.save(profile);
        auditLogUtil.audit(partnerId, "PARTNER_PROFILE_UPDATED", "profile core fields updated");
        return toDto(profile);
    }

    @Transactional
    public PartnerProfileDto updateProfileImage(Long partnerId, MultipartFile file) {
        PartnerProfile profile = getOrCreateProfile(partnerId);
        profile.setProfileImage(fileStorageUtil.storeImage(file, "partner-profile"));
        partnerProfileRepository.save(profile);
        auditLogUtil.audit(partnerId, "PARTNER_PROFILE_IMAGE_UPDATED", "profile image updated");
        return toDto(profile);
    }

    @Transactional
    public List<PartnerDocumentDto> uploadDocuments(Long partnerId,
                                                    MultipartFile aadhaar,
                                                    MultipartFile pan,
                                                    MultipartFile selfie,
                                                    MultipartFile certificate) {
        PartnerProfile partner = getOrCreateProfile(partnerId);
        uploadDocument(partner, DocumentType.AADHAAR, aadhaar);
        uploadDocument(partner, DocumentType.PAN, pan);
        uploadDocument(partner, DocumentType.SELFIE, selfie);
        if (certificate != null && !certificate.isEmpty()) {
            uploadDocument(partner, DocumentType.CERTIFICATE, certificate);
        }
        auditLogUtil.audit(partnerId, "PARTNER_KYC_UPLOADED", "documents uploaded");
        return partnerDocumentRepository.findByPartnerId(partnerId).stream()
                .map(doc -> PartnerDocumentDto.builder()
                        .id(doc.getId())
                        .type(doc.getDocumentType())
                        .url(doc.getDocumentUrl())
                        .verificationStatus(doc.getVerificationStatus())
                        .build())
                .toList();
    }

    @Transactional
    public PartnerAvailabilityResponse updateAvailability(Long partnerId, UpdateAvailabilityRequest request) {
        PartnerProfile profile = getOrCreateProfile(partnerId);
        if (profile.getAvailabilityLockedUntil() != null && profile.getAvailabilityLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Availability is locked by admin until " + profile.getAvailabilityLockedUntil());
        }
        profile.setOnline(request.online());
        partnerProfileRepository.save(profile);

        PartnerAvailability availability = partnerAvailabilityRepository.findById(partnerId)
                .orElseGet(PartnerAvailability::new);
        availability.setPartnerId(partnerId);
        availability.setOnline(request.online());
        PartnerAvailability savedAvailability = partnerAvailabilityRepository.save(availability);
        auditLogUtil.audit(partnerId, "PARTNER_AVAILABILITY_UPDATED", "online=" + request.online());
        String updatedAt = savedAvailability.getUpdatedAt().atOffset(ZoneOffset.UTC).toString();
        return new PartnerAvailabilityResponse(savedAvailability.isOnline(), updatedAt);
    }

    private void uploadDocument(PartnerProfile partner, DocumentType type, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException(type.name() + " file is required");
        }
        PartnerDocument document = new PartnerDocument();
        document.setPartner(partner);
        document.setDocumentType(type);
        document.setDocumentUrl(fileStorageUtil.storeImage(file, "partner-docs"));
        partnerDocumentRepository.save(document);
    }

    private PartnerProfile getOrCreateProfile(Long partnerId) {
        return partnerProfileRepository.findById(partnerId).orElseGet(() -> {
            PartnerProfile profile = new PartnerProfile();
            profile.setId(partnerId);
            profile.setName("Partner " + partnerId);
            String generatedMobile = "P" + partnerId;
            profile.setMobileNumber(generatedMobile);
            profile.setVerified(false);
            profile.setOnline(false);
            profile.setFullName(profile.getName());
            profile.setPhoneNumber(generatedMobile);
            profile.setAccountNumber("");
            profile.setIfscCode("");
            profile.setDeleted(false);
            profile.setExperienceYears(0);
            profile.setKycStatus(KycStatus.PENDING);
            profile.setOnboardingStatus(OnboardingStatus.PENDING);
            profile.setServiceCategoryId(resolveDefaultServiceCategoryId());
            return partnerProfileRepository.save(profile);
        });
    }

    private Long resolveDefaultServiceCategoryId() {
        return serviceCategoryLookupRepository.findAll().stream()
                .findFirst()
                .map(ServiceCategory::getId)
                .orElseGet(() -> {
                    ServiceCategory defaultCategory = new ServiceCategory();
                    defaultCategory.setName("General");
                    defaultCategory.setIcon("general");
                    return serviceCategoryLookupRepository.save(defaultCategory).getId();
                });
    }

    private PartnerProfileDto toDto(PartnerProfile profile) {
        return PartnerProfileDto.builder()
                .id(profile.getId())
                .name(profile.getName())
                .mobileNumber(profile.getMobileNumber())
                .email(profile.getEmail())
                .city(profile.getCity())
                .address(profile.getAddress())
                .serviceCategory(profile.getServiceCategory())
                .rating(profile.getRating())
                .profileImage(profile.getProfileImage())
                .isOnline(profile.isOnline())
                .isVerified(profile.isVerified())
                .createdAt(profile.getCreatedAt())
                .build();
    }
}
