package com.lexho.user_service.lexhopartner.service;

import com.lexho.user_service.admin.repository.AdminCategoryRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.lexhopartner.dto.PartnerAvailabilityResponse;
import com.lexho.user_service.lexhopartner.dto.PartnerBankDetailsDto;
import com.lexho.user_service.lexhopartner.dto.PartnerOnboardingDocumentResponse;
import com.lexho.user_service.lexhopartner.dto.PartnerOnboardingRequest;
import com.lexho.user_service.lexhopartner.dto.PartnerOnboardingResponse;
import com.lexho.user_service.lexhopartner.dto.UpdateAvailabilityRequest;
import com.lexho.user_service.lexhopartner.dto.UpdatePartnerBankDetailsRequest;
import com.lexho.user_service.lexhopartner.entity.PartnerAvailability;
import com.lexho.user_service.lexhopartner.entity.PartnerOperationalProfile;
import com.lexho.user_service.lexhopartner.model.PartnerOperationalStatus;
import com.lexho.user_service.lexhopartner.repository.PartnerAvailabilityRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerOperationalProfileRepository;
import com.lexho.user_service.lexhopartner.util.AuditLogUtil;
import com.lexho.user_service.notification.service.NotificationTriggerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class PartnerOperationalProfileService {

    private static final String STATUS_PENDING = "PENDING";
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{10}$");
    private static final Pattern PINCODE_PATTERN = Pattern.compile("^[0-9]{6}$");
    private static final int MAX_SERVICE_TYPE_SELECTIONS = 2;
    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("^[0-9]{9,18}$");
    private static final Pattern IFSC_PATTERN = Pattern.compile("^[A-Za-z]{4}0[A-Za-z0-9]{6}$");

    private final com.lexho.provider.repository.PartnerRepository dispatchPartnerRepository;
    private final PartnerOperationalProfileRepository partnerProfileRepository;
    private final PartnerAvailabilityRepository partnerAvailabilityRepository;
    private final AdminCategoryRepository categoryRepository;
    private final AuditLogUtil auditLogUtil;
    private PartnerOperationalStatusTransitionService statusTransitionService;
    private NotificationTriggerService notificationTriggerService;

    public PartnerOperationalProfileService(PartnerOperationalProfileRepository partnerProfileRepository,
                                 PartnerAvailabilityRepository partnerAvailabilityRepository,
                                 AdminCategoryRepository categoryRepository,
                                 AuditLogUtil auditLogUtil,
                                 com.lexho.provider.repository.PartnerRepository dispatchPartnerRepository) {
        this.dispatchPartnerRepository = dispatchPartnerRepository;
        this.partnerProfileRepository = partnerProfileRepository;
        this.partnerAvailabilityRepository = partnerAvailabilityRepository;
        this.categoryRepository = categoryRepository;
        this.auditLogUtil = auditLogUtil;
        this.statusTransitionService = new PartnerOperationalStatusTransitionService();
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void setStatusTransitionService(PartnerOperationalStatusTransitionService statusTransitionService) {
        this.statusTransitionService = statusTransitionService;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setNotificationTriggerService(NotificationTriggerService notificationTriggerService) {
        this.notificationTriggerService = notificationTriggerService;
    }

    @Transactional
    public void softDelete(Long partnerId) {
        PartnerOperationalProfile profile = partnerProfileRepository.findById(partnerId)
                .orElseThrow(() -> new BadRequestException("Partner not found"));
        profile.setDeleted(true);
        profile.setOnline(false);
        partnerProfileRepository.save(profile);
        syncLegacyMarketplace(profile);
    }

    @Transactional
    public PartnerOnboardingResponse onboardingDetails(Long partnerId) {
        return toOnboardingResponse(getOrCreateProfile(partnerId));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> onboardingApplications(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        Page<PartnerOperationalProfile> applications = partnerProfileRepository.findByDeletedFalse(
                PageRequest.of(safePage, safeSize,
                        Sort.by(Sort.Direction.DESC, "submittedAt").and(Sort.by(Sort.Direction.DESC, "id"))));

        return Map.of(
                "content", applications.getContent().stream().map(this::toOnboardingResponse).toList(),
                "page", applications.getNumber(),
                "size", applications.getSize(),
                "totalElements", applications.getTotalElements(),
                "totalPages", applications.getTotalPages()
        );
    }

    @Transactional
    public PartnerOnboardingResponse submitOnboarding(Long partnerId, PartnerOnboardingRequest request) {
        if (request == null) {
            throw new BadRequestException("request is required");
        }
        PartnerOperationalProfile profile = getOrCreateProfile(partnerId);
        validateRequiredOnboardingFields(request);
        List<com.lexho.user_service.admin.entity.AdminCategoryEntity> selectedCategories =
                resolveSelectedServiceTypes(request);
        Long serviceCategoryId = selectedCategories.get(0).getId();

        profile.setFullName(requiredText(request.fullName(), "fullName"));
        String phoneNumber = trimToNull(request.phoneNumber());
        if (phoneNumber != null) {
            requireMatches(phoneNumber, PHONE_PATTERN, "phoneNumber must contain 10 digits");
            if (partnerProfileRepository.existsByPhoneNumberAndIdNot(phoneNumber, partnerId)) {
                throw new BadRequestException("phoneNumber already exists");
            }
            profile.setPhoneNumber(phoneNumber);
        }
        if (request.dateOfBirth() == null) {
            throw new BadRequestException("dateOfBirth is required");
        }
        profile.setDateOfBirth(request.dateOfBirth());
        profile.setGender(requiredText(request.gender(), "gender"));
        profile.setAddressLine1(requiredText(request.addressLine1(), "addressLine1"));
        profile.setCity(requiredText(request.city(), "city"));
        profile.setState(requiredText(request.state(), "state"));
        String pincode = requiredText(request.pincode(), "pincode");
        requireMatches(pincode, PINCODE_PATTERN, "pincode must contain 6 digits");
        profile.setPincode(pincode);
        profile.setServiceCategoryId(serviceCategoryId);
        profile.getSkills().clear();
        profile.getSkills().addAll(selectedCategories.stream()
                .map(com.lexho.user_service.admin.entity.AdminCategoryEntity::getName)
                .toList());
        String accountNumber = requiredText(request.accountNumber(), "accountNumber");
        requireMatches(accountNumber, ACCOUNT_NUMBER_PATTERN, "accountNumber must contain 9 to 18 digits");
        profile.setAccountNumber(accountNumber);
        String ifscCode = requiredText(request.ifscCode(), "ifscCode");
        requireMatches(ifscCode, IFSC_PATTERN, "ifscCode is invalid");
        profile.setIfscCode(ifscCode.toUpperCase());
        profile.setBankVerificationStatus(STATUS_PENDING);
        profile.setBankRejectionReason(null);
        profile.setAadhaarFrontUrl(trimRequiredDocument(request.aadhaarFrontUrl(), "aadhaarFront"));
        profile.setAadhaarBackUrl(trimRequiredDocument(request.aadhaarBackUrl(), "aadhaarBack"));
        profile.setPanCardUrl(trimRequiredDocument(request.panCardUrl(), "panCard"));
        profile.setSelfieUrl(trimRequiredDocument(request.selfieUrl(), "selfie"));
        profile.setCertificateUrl(trimToNull(request.certificateUrl()));
        profile.setPartnerOperationalStatus(PartnerOperationalStatus.PENDING);
        profile.setDocumentStatus(STATUS_PENDING);
        profile.setRejectionReason(null);
        profile.setSubmittedAt(LocalDateTime.now());

        PartnerOperationalProfile saved = partnerProfileRepository.save(profile);
        syncLegacyMarketplace(saved);
        auditLogUtil.audit(partnerId, "PARTNER_ONBOARDING_SUBMITTED", "partner onboarding submitted");
        return toOnboardingResponse(saved);
    }

    @Transactional
    public PartnerOnboardingResponse reviewOnboarding(Long partnerId,
                                                      String applicationStatus,
                                                      String documentStatus,
                                                      String rejectionReason,
                                                      Long adminId) {
        PartnerOperationalProfile profile = partnerProfileRepository.findById(partnerId)
                .orElseThrow(() -> new BadRequestException("Partner not found"));
        PartnerOperationalStatus normalizedApplicationStatus = requireApplicationStatus(applicationStatus);
        String normalizedDocumentStatus = requireDocumentStatus(documentStatus);
        String normalizedReason = trimToNull(rejectionReason);
        boolean rejected = normalizedApplicationStatus == PartnerOperationalStatus.REJECTED
                || "REJECTED".equals(normalizedDocumentStatus);
        if (rejected && normalizedReason == null) {
            throw new BadRequestException("rejectionReason is required for rejected reviews");
        }

        profile.setPartnerOperationalStatus(normalizedApplicationStatus);
        profile.setDocumentStatus(normalizedDocumentStatus);
        profile.setRejectionReason(rejected ? normalizedReason : null);
        profile.setReviewedByAdminId(adminId);
        profile.setReviewedAt(LocalDateTime.now());
        if (normalizedApplicationStatus != PartnerOperationalStatus.APPROVED) {
            profile.setOnline(false);
        }
        PartnerOperationalProfile saved = partnerProfileRepository.save(profile);
        syncLegacyMarketplace(saved);
        auditLogUtil.audit(partnerId, "PARTNER_ONBOARDING_REVIEWED",
                "applicationStatus=" + normalizedApplicationStatus.name() + ",documentStatus=" + normalizedDocumentStatus);
        notifyPartnerOnboardingReviewed(partnerId, normalizedApplicationStatus.name(), normalizedDocumentStatus, saved.getRejectionReason());
        return toOnboardingResponse(saved);
    }

    private void notifyPartnerOnboardingReviewed(Long partnerId,
                                                 String applicationStatus,
                                                 String documentStatus,
                                                 String rejectionReason) {
        if (notificationTriggerService == null) {
            return;
        }
        notificationTriggerService.partnerOnboardingReviewed(partnerId, applicationStatus, documentStatus, rejectionReason);
    }

    @Transactional
    public PartnerBankDetailsDto bankDetails(Long partnerId) {
        return toBankDetails(getOrCreateProfile(partnerId));
    }

    @Transactional
    public PartnerBankDetailsDto updateBankDetails(Long partnerId, UpdatePartnerBankDetailsRequest request) {
        PartnerOperationalProfile profile = getOrCreateProfile(partnerId);
        profile.setAccountNumber(request.accountNumber().trim());
        profile.setIfscCode(request.ifscCode().trim().toUpperCase());
        profile.setBankVerificationStatus(STATUS_PENDING);
        profile.setBankRejectionReason(null);
        partnerProfileRepository.save(profile);
        syncLegacyMarketplace(profile);
        auditLogUtil.audit(partnerId, "PARTNER_BANK_DETAILS_UPDATED", "bank details updated");
        return toBankDetails(profile);
    }

    @Transactional
    public PartnerAvailabilityResponse updateAvailability(Long partnerId, UpdateAvailabilityRequest request) {
        PartnerOperationalProfile profile = getOrCreateProfile(partnerId);
        if (request.online()
                && (profile.getPartnerOperationalStatus() == PartnerOperationalStatus.BLOCKED
                || profile.getPartnerOperationalStatus() == PartnerOperationalStatus.REJECTED)) {
            throw new BadRequestException("Blocked or rejected partners cannot be activated");
        }
        if (profile.getAvailabilityLockedUntil() != null && profile.getAvailabilityLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Availability is locked by admin until " + profile.getAvailabilityLockedUntil());
        }
        statusTransitionService.setAvailability(profile, request.online());
        partnerProfileRepository.save(profile);
        syncLegacyMarketplace(profile);

        PartnerAvailability availability = partnerAvailabilityRepository.findById(partnerId)
                .orElseGet(PartnerAvailability::new);
        availability.setPartnerId(partnerId);
        availability.setOnline(request.online());
        PartnerAvailability savedAvailability = partnerAvailabilityRepository.save(availability);
        auditLogUtil.audit(partnerId,
                request.online() ? "SERVICE_AVAILABLE" : "SERVICE_UNAVAILABLE",
                "serviceAvailability=" + (request.online() ? "SERVICE_AVAILABLE" : "SERVICE_UNAVAILABLE"));
        String updatedAt = savedAvailability.getUpdatedAt().atOffset(ZoneOffset.UTC).toString();
        return new PartnerAvailabilityResponse(savedAvailability.isOnline(), updatedAt);
    }

    private void syncLegacyMarketplace(PartnerOperationalProfile profile) {
        dispatchPartnerRepository.findById(profile.getId()).ifPresent(legacy -> {
            legacy.setName(profile.getFullName());
            legacy.setAvailable(profile.isOnline());
            legacy.setVerificationStatus(profile.isVerified() ? "VERIFIED" : profile.getPartnerOperationalStatus().name());
            dispatchPartnerRepository.save(legacy);
        });
    }

    private PartnerOperationalProfile getOrCreateProfile(Long partnerId) {
        return partnerProfileRepository.findById(partnerId).orElseGet(() -> {
            PartnerOperationalProfile profile = new PartnerOperationalProfile();
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
            profile.setBankVerificationStatus(STATUS_PENDING);
            profile.setDocumentStatus(STATUS_PENDING);
            profile.setPartnerOperationalStatus(PartnerOperationalStatus.PENDING);
            profile.setServiceCategoryId(resolveDefaultServiceCategoryId());
            return partnerProfileRepository.save(profile);
        });
    }

    private Long resolveDefaultServiceCategoryId() {
        return categoryRepository.findByDeletedFalseOrderByDisplayOrderAscNameAsc().stream()
                .findFirst()
                .map(com.lexho.user_service.admin.entity.AdminCategoryEntity::getId)
                .orElseGet(() -> {
                    com.lexho.user_service.admin.entity.AdminCategoryEntity defaultCategory =
                            new com.lexho.user_service.admin.entity.AdminCategoryEntity();
                    defaultCategory.setName("General");
                    defaultCategory.setActive(true);
                    defaultCategory.setDeleted(false);
                    defaultCategory.setDisplayOrder(com.lexho.user_service.admin.entity.AdminCategoryEntity.DEFAULT_DISPLAY_ORDER);
                    return categoryRepository.save(defaultCategory).getId();
                });
    }

    private PartnerBankDetailsDto toBankDetails(PartnerOperationalProfile profile) {
        String account = profile.getAccountNumber();
        boolean configured = account != null && !account.isBlank()
                && profile.getIfscCode() != null && !profile.getIfscCode().isBlank();
        String masked = configured ? "*".repeat(Math.max(account.length() - 4, 0))
                + account.substring(Math.max(account.length() - 4, 0)) : null;
        return new PartnerBankDetailsDto(masked, configured ? profile.getIfscCode() : null, configured,
                profile.getBankVerificationStatus(), profile.getBankRejectionReason());
    }

    private PartnerOnboardingResponse toOnboardingResponse(PartnerOperationalProfile profile) {
        String account = profile.getAccountNumber();
        String masked = account == null || account.isBlank() ? null : "*".repeat(Math.max(account.length() - 4, 0))
                + account.substring(Math.max(account.length() - 4, 0));
        List<PartnerOnboardingDocumentResponse> documents = onboardingDocuments(profile);
        return new PartnerOnboardingResponse(
                profile.getId(),
                profile.getFullName(),
                profile.getPhoneNumber(),
                profile.getDateOfBirth(),
                profile.getGender(),
                profile.getAddressLine1(),
                profile.getCity(),
                profile.getState(),
                profile.getPincode(),
                profile.getServiceCategoryId(),
                List.copyOf(profile.getSkills()),
                masked,
                account == null || account.isBlank() ? null : account,
                account == null || account.isBlank() ? null : profile.getIfscCode(),
                profile.getBankVerificationStatus(),
                profile.getAadhaarFrontUrl(),
                profile.getAadhaarBackUrl(),
                profile.getPanCardUrl(),
                profile.getSelfieUrl(),
                profile.getCertificateUrl(),
                profile.getPartnerOperationalStatus().name(),
                profile.getDocumentStatus(),
                profile.getRejectionReason(),
                profile.getReviewedByAdminId(),
                profile.getReviewedAt(),
                profile.getSubmittedAt(),
                documents,
                documentMap(documents)
        );
    }

    private List<PartnerOnboardingDocumentResponse> onboardingDocuments(PartnerOperationalProfile profile) {
        return List.of(
                onboardingDocument("AADHAAR_FRONT", "aadhaarFront", "Aadhaar front",
                        true, profile.getAadhaarFrontUrl()),
                onboardingDocument("AADHAAR_BACK", "aadhaarBack", "Aadhaar back",
                        true, profile.getAadhaarBackUrl()),
                onboardingDocument("PAN_CARD", "panCard", "PAN card",
                        true, profile.getPanCardUrl()),
                onboardingDocument("SELFIE", "selfie", "Profile selfie",
                        true, profile.getSelfieUrl()),
                onboardingDocument("CERTIFICATE", "certificate", "Certificate",
                        false, profile.getCertificateUrl())
        );
    }

    private PartnerOnboardingDocumentResponse onboardingDocument(
            String type, String key, String label, boolean required, String url) {
        String normalizedUrl = trimToNull(url);
        boolean uploaded = normalizedUrl != null;
        return new PartnerOnboardingDocumentResponse(
                type,
                key,
                label,
                required,
                uploaded,
                normalizedUrl,
                normalizedUrl,
                normalizedUrl
        );
    }

    private Map<String, PartnerOnboardingDocumentResponse> documentMap(
            List<PartnerOnboardingDocumentResponse> documents) {
        Map<String, PartnerOnboardingDocumentResponse> map = new LinkedHashMap<>();
        for (PartnerOnboardingDocumentResponse document : documents) {
            map.put(document.key(), document);
        }
        return map;
    }

    private LinkedHashSet<String> normalizeSkills(List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            throw new BadRequestException("skills is required");
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String skill : skills) {
            String value = requiredText(skill, "skills");
            normalized.add(value);
        }
        if (normalized.isEmpty()) {
            throw new BadRequestException("skills is required");
        }
        return normalized;
    }

    private void validateRequiredOnboardingFields(PartnerOnboardingRequest request) {
        List<String> missingFields = new ArrayList<>();
        addMissing(missingFields, "fullName", request.fullName());
        if (request.dateOfBirth() == null) {
            missingFields.add("dateOfBirth");
        }
        addMissing(missingFields, "gender", request.gender());
        addMissing(missingFields, "addressLine1", request.addressLine1());
        addMissing(missingFields, "city", request.city());
        addMissing(missingFields, "state", request.state());
        addMissing(missingFields, "pincode", request.pincode());
        if (request.skills() == null || request.skills().stream().allMatch(skill -> trimToNull(skill) == null)) {
            missingFields.add("serviceTypes");
        }
        addMissing(missingFields, "accountNumber", request.accountNumber());
        addMissing(missingFields, "ifscCode", request.ifscCode());
        addMissing(missingFields, "aadhaarFront", request.aadhaarFrontUrl());
        addMissing(missingFields, "aadhaarBack", request.aadhaarBackUrl());
        addMissing(missingFields, "panCard", request.panCardUrl());
        addMissing(missingFields, "selfie", request.selfieUrl());
        if (!missingFields.isEmpty()) {
            throw new BadRequestException("Missing required onboarding fields: " + String.join(", ", missingFields));
        }
        if (!request.dateOfBirth().isBefore(java.time.LocalDate.now())) {
            throw new BadRequestException("dateOfBirth must be in the past");
        }
    }

    private List<com.lexho.user_service.admin.entity.AdminCategoryEntity> resolveSelectedServiceTypes(
            PartnerOnboardingRequest request) {
        LinkedHashSet<String> selectedValues = normalizeSkills(request.skills());
        if (selectedValues.size() > MAX_SERVICE_TYPE_SELECTIONS) {
            throw new BadRequestException("serviceTypes can contain at most 2 selections");
        }

        List<com.lexho.user_service.admin.entity.AdminCategoryEntity> activeCategories =
                categoryRepository.findByDeletedFalseOrderByDisplayOrderAscNameAsc().stream()
                        .filter(com.lexho.user_service.admin.entity.AdminCategoryEntity::isActive)
                        .toList();
        if (activeCategories.isEmpty()) {
            throw new BadRequestException("No active service types available");
        }

        List<com.lexho.user_service.admin.entity.AdminCategoryEntity> resolved = new ArrayList<>();
        for (String selectedValue : selectedValues) {
            com.lexho.user_service.admin.entity.AdminCategoryEntity category =
                    matchActiveCategory(activeCategories, selectedValue);
            if (category == null) {
                throw new BadRequestException("Invalid serviceTypes selection: " + selectedValue);
            }
            resolved.add(category);
        }

        if (request.serviceCategoryId() != null && resolved.stream()
                .noneMatch(category -> request.serviceCategoryId().equals(category.getId()))) {
            throw new BadRequestException("serviceCategoryId must match selected serviceTypes");
        }
        return resolved;
    }

    private com.lexho.user_service.admin.entity.AdminCategoryEntity matchActiveCategory(
            List<com.lexho.user_service.admin.entity.AdminCategoryEntity> activeCategories,
            String selectedValue) {
        String normalized = selectedValue.trim();
        Long selectedId = parseLongOrNull(normalized);
        for (com.lexho.user_service.admin.entity.AdminCategoryEntity category : activeCategories) {
            if (selectedId != null && selectedId.equals(category.getId())) {
                return category;
            }
            if (category.getName() != null
                    && category.getName().trim().toLowerCase(Locale.ROOT)
                    .equals(normalized.toLowerCase(Locale.ROOT))) {
                return category;
            }
        }
        return null;
    }

    private Long parseLongOrNull(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void addMissing(List<String> missingFields, String fieldName, String requestedValue) {
        if (trimToNull(requestedValue) == null) {
            missingFields.add(fieldName);
        }
    }

    private PartnerOperationalStatus requireApplicationStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new BadRequestException("applicationStatus is required");
        }
        try {
            return PartnerOperationalStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid applicationStatus");
        }
    }

    private String requireDocumentStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new BadRequestException("documentStatus is required");
        }
        String normalized = status.trim().toUpperCase();
        if (!java.util.Set.of(STATUS_PENDING, "UNDER_REVIEW", "VERIFIED", "REJECTED").contains(normalized)) {
            throw new BadRequestException("Invalid documentStatus");
        }
        return normalized;
    }

    private String requiredText(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new BadRequestException(fieldName + " is required");
        }
        return trimmed;
    }

    private void requireMatches(String value, Pattern pattern, String message) {
        if (!pattern.matcher(value).matches()) {
            throw new BadRequestException(message);
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String trimRequiredDocument(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new BadRequestException(fieldName + " is required");
        }
        return trimmed;
    }

}
