package com.lexho.user_service.legal.service;

import com.lexho.user_service.legal.dto.LegalAcceptRequest;
import com.lexho.user_service.legal.entity.LegalAcceptance;
import com.lexho.user_service.legal.repository.LegalAcceptanceRepository;
import com.lexho.user_service.legalcontent.dto.LegalDocumentResponse;
import com.lexho.user_service.legalcontent.service.LegalContentService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LegalAcceptanceServiceTest {

    @Test
    void acceptsCurrentPublishedVersionWhenRequestVersionIsMissing() {
        LegalAcceptanceRepository repository = mock(LegalAcceptanceRepository.class);
        LegalContentService legalContentService = mock(LegalContentService.class);
        LegalAcceptanceService service = new LegalAcceptanceService(repository, legalContentService);
        when(legalContentService.getPublishedByType("TERMS_AND_CONDITIONS")).thenReturn(document(3));
        when(repository.findByUserIdAndUserTypeAndDocumentTypeAndDocumentVersion(
                10L, "CUSTOMER", "TERMS_AND_CONDITIONS", 3)).thenReturn(Optional.empty());
        when(repository.save(any(LegalAcceptance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.accept(10L, "CUSTOMER", new LegalAcceptRequest("terms-and-conditions", null));

        assertThat(response.userId()).isEqualTo(10L);
        assertThat(response.documentType()).isEqualTo("TERMS_AND_CONDITIONS");
        assertThat(response.documentVersion()).isEqualTo(3);
        verify(repository).save(any(LegalAcceptance.class));
    }

    @Test
    void reusesExistingAcceptanceForSameVersion() {
        LegalAcceptanceRepository repository = mock(LegalAcceptanceRepository.class);
        LegalContentService legalContentService = mock(LegalContentService.class);
        LegalAcceptanceService service = new LegalAcceptanceService(repository, legalContentService);
        LegalAcceptance existing = new LegalAcceptance();
        existing.setUserId(10L);
        existing.setUserType("PARTNER");
        existing.setDocumentType("PRIVACY_POLICY");
        existing.setDocumentVersion(2);
        when(repository.findByUserIdAndUserTypeAndDocumentTypeAndDocumentVersion(
                10L, "PARTNER", "PRIVACY_POLICY", 2)).thenReturn(Optional.of(existing));

        var response = service.accept(10L, "PARTNER", new LegalAcceptRequest("privacy-policy", 2));

        assertThat(response.documentVersion()).isEqualTo(2);
    }

    private LegalDocumentResponse document(int version) {
        return new LegalDocumentResponse("lc_1", "terms", "Terms", "WEBSITE", "TERMS_AND_CONDITIONS",
                "PUBLISHED", "MARKDOWN", null, "Content", version, "ADMIN", "ADMIN", "ADMIN",
                null, null, null);
    }
}
