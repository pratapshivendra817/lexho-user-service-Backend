package com.lexho.user_service.legalcontent.service;

import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.audit.service.AuditLogService;
import com.lexho.user_service.legalcontent.dto.LegalDocumentRequest;
import com.lexho.user_service.legalcontent.entity.LegalDocument;
import com.lexho.user_service.legalcontent.repository.LegalDocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LegalContentServiceTest {

    private final LegalDocumentRepository repository = mock(LegalDocumentRepository.class);
    private final LegalContentService service = new LegalContentService(repository);

    @Test
    void upsertCreatesDraftWithMarkdownContent() {
        when(repository.findBySlugAndPlatformAndActiveTrue("terms-and-conditions", "PARTNER"))
                .thenReturn(Optional.empty());
        when(repository.maxVersion("TERMS_AND_CONDITIONS")).thenReturn(0);
        when(repository.save(any(LegalDocument.class))).thenAnswer(invocation -> {
            LegalDocument saved = invocation.getArgument(0);
            saved.setId(1L);
            saved.setUpdatedAt(LocalDateTime.of(2026, 8, 16, 10, 0));
            return saved;
        });

        var response = service.upsert("terms-and-conditions", request(false), 99L);

        assertThat(response.id()).isEqualTo("lc_1");
        assertThat(response.status()).isEqualTo("DRAFT");
        assertThat(response.version()).isEqualTo(1);
        assertThat(response.content()).startsWith("# Terms");
        assertThat(response.publishedAt()).isNull();
    }

    @Test
    void upsertPublishesAndIncrementsExistingVersion() {
        LegalDocument existing = document("terms-and-conditions", "PARTNER", "DRAFT", 2);
        existing.setId(7L);
        existing.setUpdatedAt(LocalDateTime.of(2026, 8, 16, 9, 0));
        when(repository.findBySlugAndPlatformAndActiveTrue("terms-and-conditions", "PARTNER"))
                .thenReturn(Optional.of(existing));
        when(repository.maxVersion("TERMS_AND_CONDITIONS")).thenReturn(2);
        when(repository.save(any(LegalDocument.class))).thenAnswer(invocation -> {
            LegalDocument saved = invocation.getArgument(0);
            saved.setId(8L);
            saved.setUpdatedAt(LocalDateTime.of(2026, 8, 16, 10, 0));
            return saved;
        });

        var response = service.upsert("terms-and-conditions", request(true), 99L);

        assertThat(response.status()).isEqualTo("PUBLISHED");
        assertThat(response.version()).isEqualTo(3);
        assertThat(response.publishedAt()).isNotNull();
    }

    @Test
    void publishedReadReturnsOnlyPublishedDocument() {
        LegalDocument published = document("terms-and-conditions", "PARTNER", "PUBLISHED", 3);
        published.setId(4L);
        published.setUpdatedAt(LocalDateTime.of(2026, 8, 16, 10, 0));
        published.setPublishedAt(LocalDateTime.of(2026, 8, 16, 10, 0));
        when(repository.findTopBySlugAndPlatformAndStatusAndCurrentPublishedTrueAndActiveTrueOrderByVersionDesc(
                "terms-and-conditions", "PARTNER", "PUBLISHED")).thenReturn(Optional.of(published));

        var response = service.getPublished("terms-and-conditions", "partner");

        assertThat(response.slug()).isEqualTo("terms-and-conditions");
        assertThat(response.status()).isEqualTo("PUBLISHED");
    }

    @Test
    void publishedReadThrowsClearNotFoundForDraftOrMissingDocument() {
        when(repository.findTopBySlugAndPlatformAndStatusAndCurrentPublishedTrueAndActiveTrueOrderByVersionDesc(
                "terms-and-conditions", "PARTNER", "PUBLISHED")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getPublished("terms-and-conditions", "PARTNER"));
    }

    @Test
    void listAndAdminGetExposeDraftAndPublishedDocuments() {
        LegalDocument draft = document("privacy-policy", "PARTNER", "DRAFT", 1);
        draft.setId(2L);
        draft.setUpdatedAt(LocalDateTime.of(2026, 8, 15, 10, 0));
        when(repository.findByPlatformAndActiveTrueOrderByUpdatedAtDesc("PARTNER")).thenReturn(List.of(draft));
        when(repository.findTopBySlugAndPlatformAndActiveTrueOrderByVersionDesc("privacy-policy", "PARTNER"))
                .thenReturn(Optional.of(draft));

        assertThat(service.list("partner")).hasSize(1);
        assertThat(service.adminGet("privacy-policy", "PARTNER").status()).isEqualTo("DRAFT");
    }

    @Test
    void validationRejectsUnsupportedPlatformFormatAndSlug() {
        assertThrows(BadRequestException.class, () -> service.list("MOBILE"));
        when(repository.maxVersion(anyString())).thenReturn(0);
        when(repository.save(any(LegalDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertThrows(BadRequestException.class, () -> service.upsert("bad slug", request(false), 1L));
        assertThrows(BadRequestException.class, () -> service.upsert("terms-and-conditions",
                new LegalDocumentRequest("Title", "PARTNER", "TERMS", "PDF", null, "content", null, false), 1L));
    }

    @Test
    void createDraftUsesTypeSlugDefaultWebsitePlatformAndDateOnlyEffectiveFrom() {
        when(repository.maxVersion("COOKIE_POLICY")).thenReturn(2);
        when(repository.save(any(LegalDocument.class))).thenAnswer(invocation -> {
            LegalDocument saved = invocation.getArgument(0);
            saved.setId(9L);
            saved.setUpdatedAt(LocalDateTime.of(2026, 8, 20, 11, 0));
            return saved;
        });

        var response = service.createDraft(new LegalDocumentRequest(
                " Cookie Policy ",
                null,
                "cookie-policy",
                null,
                "  summary  ",
                " content ",
                " note ",
                "2026-09-01",
                false
        ), null);

        assertThat(response.id()).isEqualTo("lc_9");
        assertThat(response.slug()).isEqualTo("cookie-policy");
        assertThat(response.platform()).isEqualTo("WEBSITE");
        assertThat(response.documentType()).isEqualTo("COOKIE_POLICY");
        assertThat(response.format()).isEqualTo("MARKDOWN");
        assertThat(response.summary()).isEqualTo("summary");
        assertThat(response.content()).isEqualTo("content");
        assertThat(response.version()).isEqualTo(3);
        assertThat(response.effectiveFrom()).isEqualTo(LocalDate.of(2026, 9, 1).atStartOfDay().atOffset(java.time.ZoneOffset.UTC));
        assertThat(response.updatedBy()).isEqualTo("ADMIN");
    }

    @Test
    void createDraftCanUseLegacyUserTypeWhenPlatformIsMissing() {
        when(repository.maxVersion("ABOUT_US")).thenReturn(0);
        when(repository.save(any(LegalDocument.class))).thenAnswer(invocation -> {
            LegalDocument saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        var response = service.createDraft(new LegalDocumentRequest(
                "About",
                null,
                "about-us",
                "text",
                null,
                "About Lexho",
                null,
                null,
                false,
                "general"
        ), 5L);

        assertThat(response.platform()).isEqualTo("GENERAL");
        assertThat(response.format()).isEqualTo("TEXT");
        assertThat(response.createdBy()).isEqualTo("ADMIN:5");
    }

    @Test
    void updateDraftRejectsMissingOrPublishedDocumentsAndPublishesWhenRequested() {
        when(repository.findById(404L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.updateDraft(404L, request(false), 1L));

        LegalDocument published = document("terms-and-conditions", "PARTNER", "PUBLISHED", 1);
        when(repository.findById(1L)).thenReturn(Optional.of(published));
        assertThrows(BadRequestException.class, () -> service.updateDraft(1L, request(false), 1L));

        LegalDocument draft = document("terms-and-conditions", "PARTNER", "DRAFT", 1);
        draft.setId(2L);
        when(repository.findById(2L)).thenReturn(Optional.of(draft));
        when(repository.save(draft)).thenReturn(draft);

        var response = service.updateDraft(2L, request(true), 7L);

        assertThat(response.status()).isEqualTo("PUBLISHED");
        assertThat(response.publishedBy()).isEqualTo("ADMIN:7");
        assertThat(response.effectiveFrom()).isNotNull();
        verify(repository).archiveCurrentPublished("TERMS_AND_CONDITIONS");
    }

    @Test
    void publishAndArchiveUpdateStateAndReadPublicDocumentsByMappedType() {
        LegalDocument draft = document("privacy-policy", "WEBSITE", "DRAFT", 1);
        draft.setId(3L);
        when(repository.findById(3L)).thenReturn(Optional.of(draft));
        when(repository.save(draft)).thenReturn(draft);

        var published = service.publish(3L, 12L);

        assertThat(published.status()).isEqualTo("PUBLISHED");
        assertThat(draft.isCurrentPublished()).isTrue();
        assertThat(published.publishedBy()).isEqualTo("ADMIN:12");

        var archived = service.archive(3L, 12L);

        assertThat(archived.status()).isEqualTo("ARCHIVED");
        assertThat(draft.isCurrentPublished()).isFalse();
        assertThat(draft.getArchivedAt()).isNotNull();

        LegalDocument publicDocument = document("privacy-policy", "WEBSITE", "PUBLISHED", 2);
        publicDocument.setId(4L);
        publicDocument.setDocumentType("PRIVACY_POLICY");
        publicDocument.setCurrentPublished(true);
        when(repository.findTopByDocumentTypeAndStatusAndCurrentPublishedTrueAndActiveTrueOrderByVersionDesc(
                "PRIVACY_POLICY", "PUBLISHED")).thenReturn(Optional.of(publicDocument));

        assertThat(service.getPublicDocument("privacy").documentType()).isEqualTo("PRIVACY_POLICY");
    }

    @Test
    void getByIdAndVersionsRejectMissingInactiveAndReturnDocumentHistory() {
        LegalDocument inactive = document("terms", "WEBSITE", "DRAFT", 1);
        inactive.setActive(false);
        when(repository.findById(5L)).thenReturn(Optional.of(inactive));
        assertThrows(ResourceNotFoundException.class, () -> service.getById(5L));

        LegalDocument active = document("terms", "WEBSITE", "PUBLISHED", 2);
        active.setId(6L);
        when(repository.findById(6L)).thenReturn(Optional.of(active));
        when(repository.findByDocumentTypeOrderByVersionDesc("TERMS_AND_CONDITIONS")).thenReturn(List.of(active));

        assertThat(service.getById(6L).id()).isEqualTo("lc_6");
        assertThat(service.versions(6L)).hasSize(1);
    }

    @Test
    void invalidRequiredFieldsAndEffectiveFromAreRejected() {
        assertThrows(BadRequestException.class, () -> service.getPublicDocument(" "));
        assertThrows(BadRequestException.class, () -> service.createDraft(
                new LegalDocumentRequest(" ", "WEBSITE", "TERMS", "MARKDOWN", null, "content", null, false), 1L));
        assertThrows(BadRequestException.class, () -> service.createDraft(
                new LegalDocumentRequest("Title", "WEBSITE", "TERMS", "MARKDOWN", null, " ", null, false), 1L));
        assertThrows(BadRequestException.class, () -> service.createDraft(
                new LegalDocumentRequest("Title", "WEBSITE", "TERMS", "MARKDOWN", null, "content", null,
                        "not-a-date", false), 1L));
    }

    @Test
    void auditIsCalledWhenOptionalAuditServiceIsAvailable() {
        AuditLogService auditLogService = mock(AuditLogService.class);
        ReflectionTestUtils.setField(service, "auditLogService", auditLogService);
        when(repository.maxVersion("TERMS_AND_CONDITIONS")).thenReturn(0);
        when(repository.save(any(LegalDocument.class))).thenAnswer(invocation -> {
            LegalDocument saved = invocation.getArgument(0);
            saved.setId(11L);
            return saved;
        });

        service.upsert("terms-and-conditions", request(false), 99L);

        verify(auditLogService).log(99L, "ADMIN", "LEGAL_DOCUMENT_CREATED",
                "LEGAL_DOCUMENT", "11", null, "DRAFT");
    }

    private LegalDocumentRequest request(boolean publish) {
        return new LegalDocumentRequest(
                "Terms & Conditions",
                "PARTNER",
                "TERMS_AND_CONDITIONS",
                "MARKDOWN",
                "Applicable to all partners using the Lexho partner app.",
                "# Terms & Conditions\n\nContent",
                "Updated payout clause",
                publish
        );
    }

    private LegalDocument document(String slug, String platform, String status, int version) {
        LegalDocument document = new LegalDocument();
        document.setSlug(slug);
        document.setPlatform(platform);
        document.setDocumentType("TERMS_AND_CONDITIONS");
        document.setTitle("Terms & Conditions");
        document.setFormat("MARKDOWN");
        document.setContent("# Terms");
        document.setStatus(status);
        document.setVersion(version);
        document.setActive(true);
        return document;
    }
}
