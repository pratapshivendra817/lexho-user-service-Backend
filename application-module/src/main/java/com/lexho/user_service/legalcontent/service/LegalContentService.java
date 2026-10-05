package com.lexho.user_service.legalcontent.service;

import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.audit.service.AuditLogService;
import com.lexho.user_service.legalcontent.dto.LegalDocumentRequest;
import com.lexho.user_service.legalcontent.dto.LegalDocumentResponse;
import com.lexho.user_service.legalcontent.entity.LegalDocument;
import com.lexho.user_service.legalcontent.repository.LegalDocumentRepository;
import com.lexho.user_service.util.LexhoConstants;
import com.lexho.user_service.util.StringSanitizers;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class LegalContentService {

    private static final String LEGAL_DOCUMENT_NOT_FOUND = "Legal document not found";
    private static final String PUBLISHED_LEGAL_DOCUMENT_NOT_FOUND = "Published legal document not found";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final Set<String> SUPPORTED_PLATFORMS = Set.of(
            "PARTNER", "CUSTOMER", LexhoConstants.ACTOR_ADMIN, "GENERAL", "WEBSITE");
    private static final Set<String> SUPPORTED_FORMATS = Set.of("MARKDOWN", "HTML", "TEXT");
    private static final Map<String, String> PUBLIC_TYPES = Map.of(
            "terms", "TERMS_AND_CONDITIONS",
            "privacy", "PRIVACY_POLICY",
            "cancellation", "CANCELLATION_REFUND_POLICY",
            "partner-terms", "PARTNER_TERMS_AND_CONDITIONS",
            "cookie", "COOKIE_POLICY",
            "disclaimer", "DISCLAIMER",
            "grievance", "GRIEVANCE_POLICY",
            "home", "HOME",
            "about", "ABOUT_US"
    );

    private final LegalDocumentRepository legalDocumentRepository;
    private final AuditLogService auditLogService;

    public LegalContentService(LegalDocumentRepository legalDocumentRepository) {
        this(legalDocumentRepository, Optional.empty());
    }

    @Autowired
    public LegalContentService(LegalDocumentRepository legalDocumentRepository,
                               Optional<AuditLogService> auditLogService) {
        this.legalDocumentRepository = legalDocumentRepository;
        this.auditLogService = auditLogService.orElse(null);
    }

    @Transactional(readOnly = true)
    public List<LegalDocumentResponse> list(String platform) {
        return legalDocumentRepository.findByPlatformAndActiveTrueOrderByUpdatedAtDesc(normalizePlatform(platform))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LegalDocumentResponse adminGet(String slug, String platform) {
        return legalDocumentRepository.findTopBySlugAndPlatformAndActiveTrueOrderByVersionDesc(
                        normalizeSlug(slug), normalizePlatform(platform))
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(LEGAL_DOCUMENT_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public LegalDocumentResponse getById(Long id) {
        return legalDocumentRepository.findById(id)
                .filter(LegalDocument::isActive)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(LEGAL_DOCUMENT_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public LegalDocumentResponse getPublished(String slug, String platform) {
        return legalDocumentRepository.findTopBySlugAndPlatformAndStatusAndCurrentPublishedTrueAndActiveTrueOrderByVersionDesc(
                        normalizeSlug(slug), normalizePlatform(platform), STATUS_PUBLISHED)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(PUBLISHED_LEGAL_DOCUMENT_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public LegalDocumentResponse getPublishedByType(String documentType) {
        return publishedByType(normalizeDocumentType(documentType));
    }

    private LegalDocumentResponse publishedByType(String documentType) {
        return legalDocumentRepository.findTopByDocumentTypeAndStatusAndCurrentPublishedTrueAndActiveTrueOrderByVersionDesc(
                        documentType, STATUS_PUBLISHED)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(PUBLISHED_LEGAL_DOCUMENT_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public LegalDocumentResponse getPublicDocument(String keyOrType) {
        String mapped = PUBLIC_TYPES.getOrDefault(normalizePublicKey(keyOrType), keyOrType);
        return publishedByType(normalizeDocumentType(mapped));
    }

    @Transactional(readOnly = true)
    public List<LegalDocumentResponse> versions(Long id) {
        LegalDocument document = legalDocumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(LEGAL_DOCUMENT_NOT_FOUND));
        return legalDocumentRepository.findByDocumentTypeOrderByVersionDesc(document.getDocumentType())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public LegalDocumentResponse upsert(String slug, LegalDocumentRequest request, Long adminId) {
        String documentType = normalizeDocumentType(request.documentType());
        LegalDocument document = new LegalDocument();
        document.setSlug(normalizeSlug(slug));
        document.setPlatform(normalizePlatform(effectivePlatform(request)));
        document.setDocumentType(documentType);
        document.setVersion(legalDocumentRepository.maxVersion(documentType) + 1);
        document.setCreatedBy(actor(adminId));
        applyDraftFields(document, request, adminId);
        if (Boolean.TRUE.equals(request.publish())) {
            publishDocument(document, adminId);
        } else {
            document.setStatus(STATUS_DRAFT);
        }
        LegalDocument saved = legalDocumentRepository.save(document);
        audit(adminId, "LEGAL_DOCUMENT_CREATED", saved, null, saved.getStatus());
        return toResponse(saved);
    }

    @Transactional
    public LegalDocumentResponse createDraft(LegalDocumentRequest request, Long adminId) {
        String documentType = normalizeDocumentType(request.documentType());
        String slug = normalizeSlug(slugFor(documentType));
        LegalDocument document = new LegalDocument();
        document.setSlug(slug);
        document.setPlatform(normalizePlatform(effectivePlatform(request) == null ? "WEBSITE" : effectivePlatform(request)));
        document.setDocumentType(documentType);
        document.setVersion(legalDocumentRepository.maxVersion(documentType) + 1);
        document.setCreatedBy(actor(adminId));
        applyDraftFields(document, request, adminId);
        document.setStatus(STATUS_DRAFT);
        LegalDocument saved = legalDocumentRepository.save(document);
        audit(adminId, "LEGAL_DOCUMENT_CREATED", saved, null, saved.getStatus());
        return toResponse(saved);
    }

    @Transactional
    public LegalDocumentResponse updateDraft(Long id, LegalDocumentRequest request, Long adminId) {
        LegalDocument document = legalDocumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(LEGAL_DOCUMENT_NOT_FOUND));
        if (!STATUS_DRAFT.equalsIgnoreCase(document.getStatus())) {
            throw new BadRequestException("Only draft legal documents can be edited");
        }
        applyDraftFields(document, request, adminId);
        if (Boolean.TRUE.equals(request.publish())) {
            publishDocument(document, adminId);
        }
        LegalDocument saved = legalDocumentRepository.save(document);
        audit(adminId, "LEGAL_DOCUMENT_UPDATED", saved, STATUS_DRAFT, saved.getStatus());
        return toResponse(saved);
    }

    @Transactional
    public LegalDocumentResponse publish(Long id, Long adminId) {
        LegalDocument document = legalDocumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(LEGAL_DOCUMENT_NOT_FOUND));
        publishDocument(document, adminId);
        LegalDocument saved = legalDocumentRepository.save(document);
        audit(adminId, "LEGAL_DOCUMENT_PUBLISHED", saved, null, saved.getStatus());
        return toResponse(saved);
    }

    @Transactional
    public LegalDocumentResponse archive(Long id, Long adminId) {
        LegalDocument document = legalDocumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(LEGAL_DOCUMENT_NOT_FOUND));
        document.setStatus("ARCHIVED");
        document.setCurrentPublished(false);
        document.setArchivedAt(LocalDateTime.now());
        document.setUpdatedBy(actor(adminId));
        LegalDocument saved = legalDocumentRepository.save(document);
        audit(adminId, "LEGAL_DOCUMENT_ARCHIVED", saved, null, saved.getStatus());
        return toResponse(saved);
    }

    private void applyDraftFields(LegalDocument document, LegalDocumentRequest request, Long adminId) {
        document.setTitle(normalizeRequired(request.title(), "title is required"));
        document.setFormat(normalizeFormat(request.format() == null ? "MARKDOWN" : request.format()));
        document.setSummary(normalize(request.summary()));
        document.setContent(normalizeRequired(request.content(), "content is required"));
        document.setChangeNote(normalize(request.changeNote()));
        document.setEffectiveFrom(parseDateTime(request.effectiveFrom()));
        document.setUpdatedBy(actor(adminId));
        document.setActive(true);
    }

    private String effectivePlatform(LegalDocumentRequest request) {
        return request.platform() == null ? request.userType() : request.platform();
    }

    private void publishDocument(LegalDocument document, Long adminId) {
        legalDocumentRepository.archiveCurrentPublished(document.getDocumentType());
        LocalDateTime now = LocalDateTime.now();
        document.setStatus(STATUS_PUBLISHED);
        document.setCurrentPublished(true);
        document.setPublishedAt(now);
        document.setPublishedBy(actor(adminId));
        if (document.getEffectiveFrom() == null) {
            document.setEffectiveFrom(now);
        }
    }

    private LegalDocumentResponse toResponse(LegalDocument document) {
        return new LegalDocumentResponse(
                "lc_" + document.getId(),
                document.getSlug(),
                document.getTitle(),
                document.getPlatform(),
                document.getDocumentType(),
                document.getStatus(),
                document.getFormat(),
                document.getSummary(),
                document.getContent(),
                document.getVersion(),
                document.getCreatedBy(),
                document.getUpdatedBy(),
                document.getPublishedBy(),
                toOffset(document.getEffectiveFrom()),
                toOffset(document.getUpdatedAt()),
                toOffset(document.getPublishedAt())
        );
    }

    private OffsetDateTime toOffset(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    private String normalizeSlug(String slug) {
        String normalized = normalizeRequired(slug, "slug is required").toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9]+(?:-[a-z0-9]+)*")) {
            throw new BadRequestException("Invalid slug");
        }
        return normalized;
    }

    private String normalizePublicKey(String value) {
        return normalizeRequired(value, "documentType is required").toLowerCase(Locale.ROOT);
    }

    private String normalizePlatform(String platform) {
        String normalized = normalizeRequired(platform, "platform is required").toUpperCase(Locale.ROOT);
        if (!SUPPORTED_PLATFORMS.contains(normalized)) {
            throw new BadRequestException("Unsupported platform");
        }
        return normalized;
    }

    private String normalizeFormat(String format) {
        String normalized = normalizeRequired(format, "format is required").toUpperCase(Locale.ROOT);
        if (!SUPPORTED_FORMATS.contains(normalized)) {
            throw new BadRequestException("Unsupported legal document format");
        }
        return normalized;
    }

    private String normalizeDocumentType(String documentType) {
        return normalizeRequired(documentType, "documentType is required")
                .toUpperCase(Locale.ROOT)
                .replace('-', '_');
    }

    private String normalizeRequired(String value, String message) {
        return StringSanitizers.requireTrimmed(value, message);
    }

    private String normalize(String value) {
        return StringSanitizers.trimToNull(value);
    }

    private LocalDateTime parseDateTime(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(normalized).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(normalized);
            } catch (DateTimeParseException exception) {
                try {
                    return LocalDate.parse(normalized).atStartOfDay();
                } catch (DateTimeParseException dateException) {
                    throw new BadRequestException("effectiveFrom must be an ISO date-time");
                }
            }
        }
    }

    private String actor(Long adminId) {
        return adminId == null ? LexhoConstants.ACTOR_ADMIN : LexhoConstants.ACTOR_ADMIN_PREFIX + adminId;
    }

    private String slugFor(String documentType) {
        return documentType.toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private void audit(Long adminId, String action, LegalDocument document, String oldValue, String newValue) {
        if (auditLogService != null) {
        auditLogService.log(adminId, LexhoConstants.ACTOR_ADMIN, action, "LEGAL_DOCUMENT",
                    document.getId() == null ? null : document.getId().toString(), oldValue, newValue);
        }
    }
}
