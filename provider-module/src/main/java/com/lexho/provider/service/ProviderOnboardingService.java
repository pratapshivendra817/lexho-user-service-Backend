package com.lexho.provider.service;

import com.lexho.catalog.entity.ServiceCategory;
import com.lexho.catalog.repository.CategoryRepository;
import com.lexho.provider.dto.ProviderActionResponse;
import com.lexho.provider.dto.ProviderDetailsResponse;
import com.lexho.provider.dto.ProviderDocumentUploadResponse;
import com.lexho.provider.dto.ProviderListItemResponse;
import com.lexho.provider.dto.ProviderOnboardingRequest;
import com.lexho.provider.dto.ProviderPageResponse;
import com.lexho.provider.dto.ProviderStatsResponse;
import com.lexho.provider.entity.Provider;
import com.lexho.provider.entity.ProviderDocument;
import com.lexho.provider.enums.KycStatus;
import com.lexho.provider.enums.OnboardingStatus;
import com.lexho.provider.enums.ProviderDocumentType;
import com.lexho.provider.exception.DocumentNotFoundException;
import com.lexho.provider.exception.ProviderNotFoundException;
import com.lexho.provider.exception.ProviderOperationException;
import com.lexho.provider.exception.ProviderValidationException;
import com.lexho.provider.mapper.ProviderMapper;
import com.lexho.provider.repository.ProviderDocumentRepository;
import com.lexho.provider.repository.ProviderRepository;
import com.lexho.provider.service.storage.DocumentStorageService;
import com.lexho.provider.service.storage.StoredDocument;
import com.lexho.user.entity.User;
import com.lexho.user.repository.UserRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ProviderOnboardingService {

    private static final Logger log = LoggerFactory.getLogger(ProviderOnboardingService.class);

    private static final Set<ProviderDocumentType> MANDATORY_DOCUMENTS = Set.of(
            ProviderDocumentType.AADHAAR_FRONT,
            ProviderDocumentType.AADHAAR_BACK,
            ProviderDocumentType.PAN,
            ProviderDocumentType.PROFILE_PHOTO
    );

    private final ProviderRepository providerRepository;
    private final ProviderDocumentRepository providerDocumentRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ProviderMapper providerMapper;
    private final DocumentStorageService documentStorageService;

    public ProviderOnboardingService(ProviderRepository providerRepository,
                                     ProviderDocumentRepository providerDocumentRepository,
                                     CategoryRepository categoryRepository,
                                     UserRepository userRepository,
                                     ProviderMapper providerMapper,
                                     DocumentStorageService documentStorageService) {
        this.providerRepository = providerRepository;
        this.providerDocumentRepository = providerDocumentRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.providerMapper = providerMapper;
        this.documentStorageService = documentStorageService;
    }

    @Transactional
    public ProviderDetailsResponse submitOnboarding(ProviderOnboardingRequest request) {
        String email = normalizeRequired(request.getEmail(), "Email is required");
        String phone = normalizeRequired(request.getPhoneNumber(), "Phone number is required");
        if (providerRepository.existsByEmailIgnoreCaseAndDeletedFalse(email)) {
            throw new ProviderValidationException("Provider already exists with email: " + email);
        }
        if (providerRepository.existsByPhoneNumberAndDeletedFalse(phone)) {
            throw new ProviderValidationException("Provider already exists with phone number: " + phone);
        }

        ServiceCategory category = categoryRepository.findById(request.getServiceCategoryId())
                .orElseThrow(() -> new ProviderValidationException("Service category not found: " + request.getServiceCategoryId()));

        Provider provider = new Provider();
        provider.setFullName(normalizeRequired(request.getFullName(), "Full name is required"));
        provider.setEmail(email.toLowerCase(Locale.ROOT));
        provider.setPhoneNumber(phone);
        provider.setGender(normalize(request.getGender()));
        provider.setDateOfBirth(request.getDateOfBirth());
        provider.setCurrentCity(normalize(request.getCurrentCity()));
        provider.setCurrentState(normalize(request.getCurrentState()));
        provider.setCurrentFullAddress(normalize(request.getCurrentFullAddress()));
        provider.setCity(normalizeRequired(request.getCity(), "City is required"));
        provider.setState(normalizeRequired(request.getState(), "State is required"));
        provider.setFullAddress(normalizeRequired(request.getFullAddress(), "Full address is required"));
        provider.setServiceCategory(category);
        provider.setExperienceYears(request.getExperienceYears());
        provider.setServiceAreas(normalizeAreas(request.getServiceAreas()));
        provider.setAccountNumber(normalizeRequired(request.getAccountNumber(), "Account number is required"));
        provider.setIfscCode(normalizeRequired(request.getIfscCode(), "IFSC code is required").toUpperCase(Locale.ROOT));
        provider.setKycStatus(KycStatus.PENDING);
        provider.setOnboardingStatus(OnboardingStatus.PENDING);
        provider.setDeleted(false);
        provider.setUser(resolveLinkedUserByPhone(phone));

        Provider saved = providerRepository.save(provider);
        if (saved.getProviderCode() == null || saved.getProviderCode().isBlank()) {
            saved.setProviderCode("PRV" + (1000 + saved.getId()));
            saved = providerRepository.save(saved);
        }

        log.info("Provider onboarding submitted id={} code={}", saved.getId(), saved.getProviderCode());
        return providerMapper.toDetails(saved, providerDocumentRepository.findByProvider_IdOrderByUploadedAtDesc(saved.getId()));
    }

    @Transactional(readOnly = true)
    public ProviderStatsResponse getStats() {
        return new ProviderStatsResponse(
                providerRepository.countByOnboardingStatusAndDeletedFalse(OnboardingStatus.PENDING),
                providerRepository.countByOnboardingStatusAndDeletedFalse(OnboardingStatus.APPROVED),
                providerRepository.countByOnboardingStatusAndDeletedFalse(OnboardingStatus.REJECTED),
                providerRepository.countByOnboardingStatusAndDeletedFalse(OnboardingStatus.BLOCKED)
        );
    }

    @Transactional(readOnly = true)
    public ProviderPageResponse<ProviderListItemResponse> getProviders(int page,
                                                                       int size,
                                                                       String sortBy,
                                                                       String sortDir,
                                                                       String status,
                                                                       String serviceType,
                                                                       String city,
                                                                       String search) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by("asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC, normalizeSortBy(sortBy))
        );
        OnboardingStatus statusFilter = parseStatus(status);
        String normalizedServiceType = normalize(serviceType);
        String normalizedCity = normalize(city);
        String normalizedSearch = normalize(search);

        Specification<Provider> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("deleted")));

            if (statusFilter != null) {
                predicates.add(cb.equal(root.get("onboardingStatus"), statusFilter));
            }
            if (normalizedCity != null) {
                predicates.add(cb.like(cb.lower(root.get("city")), "%" + normalizedCity.toLowerCase(Locale.ROOT) + "%"));
            }
            if (normalizedServiceType != null && !"ALL".equalsIgnoreCase(normalizedServiceType)) {
                predicates.add(cb.like(
                        cb.lower(root.join("serviceCategory", JoinType.LEFT).get("name")),
                        "%" + normalizedServiceType.toLowerCase(Locale.ROOT) + "%"
                ));
            }
            if (normalizedSearch != null) {
                String like = "%" + normalizedSearch.toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), like),
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(cb.lower(root.get("phoneNumber")), like),
                        cb.like(cb.lower(root.get("providerCode")), like)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Provider> providers = providerRepository.findAll(specification, pageable);
        List<ProviderListItemResponse> content = providers.getContent().stream()
                .map(providerMapper::toListItem)
                .toList();
        return new ProviderPageResponse<>(content, providers.getTotalElements(), providers.getTotalPages(), providers.getNumber());
    }

    @Transactional(readOnly = true)
    public ProviderDetailsResponse getProviderDetails(Long id) {
        Provider provider = getProvider(id);
        List<ProviderDocument> documents = providerDocumentRepository.findByProvider_IdOrderByUploadedAtDesc(id);
        return providerMapper.toDetails(provider, documents);
    }

    @Transactional
    public ProviderActionResponse approveProvider(Long id) {
        Provider provider = getProvider(id);
        if (provider.getOnboardingStatus() != OnboardingStatus.PENDING) {
            throw new ProviderOperationException("Only pending providers can be approved");
        }
        validateMandatoryDocuments(id);
        provider.setOnboardingStatus(OnboardingStatus.APPROVED);
        provider.setRejectionReason(null);
        provider.setBlockedReason(null);
        provider.setKycStatus(KycStatus.VERIFIED);
        provider.setKycFailedReason(null);
        Provider saved = providerRepository.save(provider);
        return new ProviderActionResponse(saved.getId(), saved.getProviderCode(), saved.getOnboardingStatus(), "Provider approved");
    }

    @Transactional
    public ProviderActionResponse rejectProvider(Long id, String reason) {
        Provider provider = getProvider(id);
        if (provider.getOnboardingStatus() != OnboardingStatus.PENDING) {
            throw new ProviderOperationException("Only pending providers can be rejected");
        }
        provider.setOnboardingStatus(OnboardingStatus.REJECTED);
        provider.setRejectionReason(normalizeRequired(reason, "Rejection reason is required"));
        provider.setBlockedReason(null);
        provider.setKycStatus(KycStatus.FAILED);
        provider.setKycFailedReason(provider.getRejectionReason());
        Provider saved = providerRepository.save(provider);
        return new ProviderActionResponse(saved.getId(), saved.getProviderCode(), saved.getOnboardingStatus(), "Provider rejected");
    }

    @Transactional
    public ProviderActionResponse blockProvider(Long id, String reason) {
        Provider provider = getProvider(id);
        provider.setOnboardingStatus(OnboardingStatus.BLOCKED);
        provider.setBlockedReason(normalizeRequired(reason, "Blocked reason is required"));
        Provider saved = providerRepository.save(provider);
        return new ProviderActionResponse(saved.getId(), saved.getProviderCode(), saved.getOnboardingStatus(), "Provider blocked");
    }

    @Transactional
    public ProviderActionResponse unblockProvider(Long id) {
        Provider provider = getProvider(id);
        if (provider.getOnboardingStatus() != OnboardingStatus.BLOCKED) {
            throw new ProviderOperationException("Only blocked providers can be unblocked");
        }
        provider.setOnboardingStatus(OnboardingStatus.APPROVED);
        provider.setBlockedReason(null);
        Provider saved = providerRepository.save(provider);
        return new ProviderActionResponse(saved.getId(), saved.getProviderCode(), saved.getOnboardingStatus(), "Provider unblocked");
    }

    @Transactional
    public void softDeleteProvider(Long id) {
        Provider provider = getProvider(id);
        provider.setDeleted(true);
        providerRepository.save(provider);
    }

    @Transactional
    public ProviderDocumentUploadResponse uploadDocument(Long providerId,
                                                         ProviderDocumentType documentType,
                                                         MultipartFile file) {
        Provider provider = getProvider(providerId);
        validateDocumentFileType(documentType, file);
        StoredDocument storedDocument = documentStorageService.store(providerId, documentType, file);

        ProviderDocument providerDocument = new ProviderDocument();
        providerDocument.setProvider(provider);
        providerDocument.setDocumentType(documentType);
        providerDocument.setFileImage(storedDocument.getPath());
        providerDocument.setFileName(storedDocument.getFileName());
        providerDocument.setFileType(storedDocument.getContentType());

        ProviderDocument saved = providerDocumentRepository.save(providerDocument);
        return new ProviderDocumentUploadResponse(
                saved.getId(),
                providerId,
                saved.getDocumentType(),
                saved.getFileImage(),
                saved.getFileName()
        );
    }

    @Transactional(readOnly = true)
    public ProviderDocument getDocument(Long documentId) {
        return providerDocumentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
    }

    @Transactional(readOnly = true)
    public ProviderDocument getProviderDocument(Long providerId, Long documentId) {
        return providerDocumentRepository.findByIdAndProvider_Id(documentId, providerId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
    }

    private void validateMandatoryDocuments(Long providerId) {
        for (ProviderDocumentType documentType : MANDATORY_DOCUMENTS) {
            if (!providerDocumentRepository.existsByProvider_IdAndDocumentType(providerId, documentType)) {
                throw new ProviderOperationException("Mandatory document missing: " + documentType.name());
            }
        }
    }

    private void validateDocumentFileType(ProviderDocumentType documentType, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ProviderValidationException("Document file is required");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (documentType == ProviderDocumentType.PROFILE_PHOTO) {
            if (!contentType.equals("image/jpeg") && !contentType.equals("image/jpg")) {
                throw new ProviderValidationException("PROFILE_PHOTO must be a JPG image");
            }
            return;
        }
        if (!contentType.equals("application/pdf")) {
            throw new ProviderValidationException(documentType.name() + " must be a PDF file");
        }
    }

    private Provider getProvider(Long id) {
        return providerRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ProviderNotFoundException(id));
    }

    private User resolveLinkedUserByPhone(String phone) {
        return userRepository.findByMobile(phone).orElse(null);
    }

    private OnboardingStatus parseStatus(String statusText) {
        String normalized = normalize(statusText);
        if (normalized == null) {
            return null;
        }
        try {
            return OnboardingStatus.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ProviderValidationException("Invalid onboarding status: " + statusText);
        }
    }

    private String normalizeSortBy(String sortBy) {
        String normalized = normalize(sortBy);
        if (normalized == null) {
            return "createdAt";
        }
        return switch (normalized) {
            case "createdAt", "updatedAt", "fullName", "city", "onboardingStatus", "providerCode" -> normalized;
            default -> "createdAt";
        };
    }

    private Set<String> normalizeAreas(List<String> serviceAreas) {
        Set<String> normalized = new HashSet<>();
        if (serviceAreas == null) {
            return normalized;
        }
        for (String area : serviceAreas) {
            String cleaned = normalize(area);
            if (cleaned != null) {
                normalized.add(cleaned);
            }
        }
        if (normalized.isEmpty()) {
            throw new ProviderValidationException("At least one service area is required");
        }
        return normalized;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new ProviderValidationException(message);
        }
        return normalized;
    }
}

