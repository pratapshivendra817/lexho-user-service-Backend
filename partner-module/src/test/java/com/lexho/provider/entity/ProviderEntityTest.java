package com.lexho.provider.entity;

import com.lexho.catalog.entity.ServiceCategory;
import com.lexho.provider.enums.KycStatus;
import com.lexho.provider.enums.OnboardingStatus;
import com.lexho.provider.enums.ProviderDocumentType;
import com.lexho.user.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProviderEntityTest {

    @Test
    void providerLifecycleSetsDefaultsAndRefreshesTimestamp() {
        Provider provider = new Provider();
        provider.onCreate();

        assertEquals(KycStatus.PENDING, provider.getKycStatus());
        assertEquals(OnboardingStatus.PENDING, provider.getOnboardingStatus());
        assertEquals(provider.getCreatedAt(), provider.getUpdatedAt());
        LocalDateTime old = LocalDateTime.of(2020, 1, 1, 0, 0);
        provider.setUpdatedAt(old);
        provider.onUpdate();
        assertTrue(provider.getUpdatedAt().isAfter(old));
    }

    @Test
    void providerLifecyclePreservesExplicitStatuses() {
        Provider provider = new Provider();
        provider.setKycStatus(KycStatus.VERIFIED);
        provider.setOnboardingStatus(OnboardingStatus.APPROVED);

        provider.onCreate();

        assertEquals(KycStatus.VERIFIED, provider.getKycStatus());
        assertEquals(OnboardingStatus.APPROVED, provider.getOnboardingStatus());
    }

    @Test
    void providerAccessorsAndDefensiveCollectionsCoverAllFields() {
        Provider provider = new Provider();
        ServiceCategory category = new ServiceCategory();
        User user = new User();
        ProviderDocument document = new ProviderDocument();
        LocalDate dob = LocalDate.of(1990, 1, 1);
        LocalDateTime time = LocalDateTime.of(2026, 1, 1, 10, 0);
        Set<String> areas = new HashSet<>(Set.of("Delhi"));
        List<ProviderDocument> documents = new ArrayList<>(List.of(document));

        provider.setId(1L); provider.setProviderCode("PRV1"); provider.setFullName("Name");
        provider.setEmail("e@x.com"); provider.setPhoneNumber("999"); provider.setGender("F");
        provider.setDateOfBirth(dob); provider.setCurrentCity("Delhi"); provider.setCurrentState("Delhi");
        provider.setCurrentFullAddress("Current"); provider.setCity("Delhi"); provider.setState("Delhi");
        provider.setFullAddress("Full"); provider.setServiceCategory(category); provider.setExperienceYears(5);
        provider.setServiceAreas(areas); provider.setAccountNumber("1234"); provider.setIfscCode("IFSC");
        provider.setKycStatus(KycStatus.FAILED); provider.setKycFailedReason("bad");
        provider.setOnboardingStatus(OnboardingStatus.BLOCKED); provider.setRejectionReason("reject");
        provider.setBlockedReason("block"); provider.setOnline(true); provider.setUser(user);
        provider.setDocuments(documents); provider.setCreatedAt(time); provider.setUpdatedAt(time); provider.setDeleted(true);

        assertEquals(1L, provider.getId()); assertEquals("PRV1", provider.getProviderCode());
        assertEquals("Name", provider.getFullName()); assertEquals("e@x.com", provider.getEmail());
        assertEquals("999", provider.getPhoneNumber()); assertEquals("F", provider.getGender());
        assertEquals(dob, provider.getDateOfBirth()); assertEquals("Delhi", provider.getCurrentCity());
        assertEquals("Delhi", provider.getCurrentState()); assertEquals("Current", provider.getCurrentFullAddress());
        assertEquals("Delhi", provider.getCity()); assertEquals("Delhi", provider.getState());
        assertEquals("Full", provider.getFullAddress()); assertEquals(category, provider.getServiceCategory());
        assertEquals(5, provider.getExperienceYears()); assertEquals(Set.of("Delhi"), provider.getServiceAreas());
        assertNotSame(areas, provider.getServiceAreas()); assertEquals("1234", provider.getAccountNumber());
        assertEquals("IFSC", provider.getIfscCode()); assertEquals("bad", provider.getKycFailedReason());
        assertEquals("reject", provider.getRejectionReason()); assertEquals("block", provider.getBlockedReason());
        assertTrue(provider.isOnline()); assertEquals(user, provider.getUser());
        assertEquals(List.of(document), provider.getDocuments()); assertNotSame(documents, provider.getDocuments());
        assertEquals(time, provider.getCreatedAt()); assertEquals(time, provider.getUpdatedAt()); assertTrue(provider.isDeleted());

        provider.setServiceAreas(null); provider.setDocuments(null);
        assertTrue(provider.getServiceAreas().isEmpty()); assertTrue(provider.getDocuments().isEmpty());
    }

    @Test
    void providerDocumentLifecycleAndAccessorsWork() {
        Provider provider = new Provider();
        ProviderDocument document = new ProviderDocument();
        document.setId(2L); document.setProvider(provider); document.setDocumentType(ProviderDocumentType.PAN);
        document.setFileImage("/pan"); document.setFileName("pan.pdf"); document.setFileType("application/pdf");
        document.onCreate();

        assertEquals(2L, document.getId()); assertEquals(provider, document.getProvider());
        assertEquals(ProviderDocumentType.PAN, document.getDocumentType()); assertEquals("/pan", document.getFileImage());
        assertEquals("pan.pdf", document.getFileName()); assertEquals("application/pdf", document.getFileType());
        assertNotNull(document.getUploadedAt());
    }

    @Test
    void legacyPartnerAccessorsAndRatingAliasesWork() {
        Partner partner = new Partner();
        partner.setId(1L); partner.setName("Chef"); partner.setCategoryId(2L); partner.setSubCategoryId(3L);
        partner.setRating(4.5); partner.setTotalReviews(null); partner.setPrice(500.0); partner.setAvailable(true);
        partner.setExperience(4); partner.setImage("img"); partner.setVerificationStatus("VERIFIED");

        assertEquals(1L, partner.getId()); assertEquals("Chef", partner.getName());
        assertEquals(2L, partner.getCategoryId()); assertEquals(3L, partner.getSubCategoryId());
        assertEquals(4.5, partner.getRating()); assertEquals(4.5, partner.getAverageRating());
        assertEquals(0L, partner.getTotalReviews()); assertEquals(500.0, partner.getPrice());
        assertTrue(partner.getAvailable()); assertEquals(4, partner.getExperience());
        assertEquals("img", partner.getImage()); assertEquals("VERIFIED", partner.getVerificationStatus());
        partner.setAverageRating(4.8); partner.setTotalReviews(10L);
        assertEquals(4.8, partner.getRating()); assertEquals(10L, partner.getTotalReviews());
    }
}
