package com.lexho.provider.mapper;

import com.lexho.catalog.entity.ServiceCategory;
import com.lexho.provider.dto.ProviderDetailsResponse;
import com.lexho.provider.dto.ProviderDocumentResponse;
import com.lexho.provider.dto.ProviderListItemResponse;
import com.lexho.provider.entity.Provider;
import com.lexho.provider.entity.ProviderDocument;
import com.lexho.provider.enums.KycStatus;
import com.lexho.provider.enums.OnboardingStatus;
import com.lexho.provider.enums.ProviderDocumentType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class ProviderMapperTest {

    private final ProviderMapper mapper = new ProviderMapper();

    @Test
    void toListItemMapsProviderSummary() {
        Provider provider = provider();

        ProviderListItemResponse result = mapper.toListItem(provider);

        assertEquals(1L, result.getId());
        assertEquals("PRV1001", result.getProviderCode());
        assertEquals("Asha", result.getFullName());
        assertEquals("Cooking", result.getServiceType());
        assertEquals(KycStatus.VERIFIED, result.getKycStatus());
        assertEquals(OnboardingStatus.APPROVED, result.getOnboardingStatus());
    }

    @Test
    void toDetailsMapsAllFieldsDocumentsAndMasksLongAccount() {
        Provider provider = provider();
        ProviderDocument document = document();

        ProviderDetailsResponse result = mapper.toDetails(provider, List.of(document));

        assertEquals(1L, result.getId());
        assertEquals("Female", result.getGender());
        assertEquals(LocalDate.of(1995, 1, 1), result.getDateOfBirth());
        assertEquals(10L, result.getServiceCategoryId());
        assertEquals(Set.of("Delhi", "Noida"), Set.copyOf(result.getServiceAreas()));
        assertEquals("XXXXXX5678", result.getAccountNumberMasked());
        assertEquals(1, result.getDocuments().size());
        assertEquals("application/pdf", result.getDocuments().get(0).getContentType());
        assertNotSame(provider.getServiceAreas(), result.getServiceAreas());
    }

    @Test
    void toDetailsMasksNullAndShortAccountsAndSupportsEmptyDocuments() {
        Provider provider = provider();
        provider.setAccountNumber(null);
        assertEquals("****", mapper.toDetails(provider, List.of()).getAccountNumberMasked());
        provider.setAccountNumber("123");
        assertEquals("****", mapper.toDetails(provider, List.of()).getAccountNumberMasked());
    }

    @Test
    void toDocumentMapsEveryMetadataField() {
        ProviderDocumentResponse result = mapper.toDocument(document());

        assertEquals(20L, result.getId());
        assertEquals(ProviderDocumentType.PAN, result.getDocumentType());
        assertEquals("stored/pan.pdf", result.getFileImage());
        assertEquals("pan.pdf", result.getFileName());
        assertEquals("application/pdf", result.getFileType());
        assertEquals(LocalDateTime.of(2026, 6, 1, 10, 0), result.getUploadedAt());
    }

    private Provider provider() {
        ServiceCategory category = new ServiceCategory();
        category.setId(10L);
        category.setName("Cooking");
        Provider provider = new Provider();
        provider.setId(1L);
        provider.setProviderCode("PRV1001");
        provider.setFullName("Asha");
        provider.setEmail("asha@example.com");
        provider.setPhoneNumber("9876543210");
        provider.setGender("Female");
        provider.setDateOfBirth(LocalDate.of(1995, 1, 1));
        provider.setCurrentCity("Delhi");
        provider.setCurrentState("Delhi");
        provider.setCurrentFullAddress("Sector 1");
        provider.setCity("Delhi");
        provider.setState("Delhi");
        provider.setFullAddress("Sector 1");
        provider.setServiceCategory(category);
        provider.setExperienceYears(4);
        provider.setServiceAreas(Set.of("Delhi", "Noida"));
        provider.setAccountNumber("12345678");
        provider.setIfscCode("HDFC0001234");
        provider.setKycStatus(KycStatus.VERIFIED);
        provider.setOnboardingStatus(OnboardingStatus.APPROVED);
        provider.setOnline(true);
        ReflectionTestUtils.setField(provider, "createdAt", LocalDateTime.of(2026, 6, 1, 9, 0));
        ReflectionTestUtils.setField(provider, "updatedAt", LocalDateTime.of(2026, 6, 2, 9, 0));
        return provider;
    }

    private ProviderDocument document() {
        ProviderDocument document = new ProviderDocument();
        document.setId(20L);
        document.setDocumentType(ProviderDocumentType.PAN);
        document.setFileImage("stored/pan.pdf");
        document.setFileName("pan.pdf");
        document.setFileType("application/pdf");
        document.setUploadedAt(LocalDateTime.of(2026, 6, 1, 10, 0));
        return document;
    }
}
