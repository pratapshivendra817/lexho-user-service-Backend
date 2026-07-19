package com.lexho.provider.dto;

import com.lexho.provider.enums.KycStatus;
import com.lexho.provider.enums.OnboardingStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderDtoBranchTest {

    @Test
    void onboardingRequestShouldDefensivelyCopyServiceAreasAndTypes() {
        ProviderOnboardingRequest request = new ProviderOnboardingRequest();

        assertThat(request.getServiceAreas()).isEmpty();
        assertThat(request.getServiceType()).isEmpty();

        request.setServiceAreas(null);
        request.setServiceType(null);
        assertThat(request.getServiceAreas()).isEmpty();
        assertThat(request.getServiceType()).isEmpty();

        request.setServiceAreas(List.of("Delhi"));
        request.setServiceType(List.of("Cook"));
        List<String> areas = request.getServiceAreas();
        List<String> serviceTypes = request.getServiceType();

        areas.add("Noida");
        serviceTypes.add("Cleaning");

        assertThat(request.getServiceAreas()).containsExactly("Delhi");
        assertThat(request.getServiceType()).containsExactly("Cook");
    }

    @Test
    void onboardingRequestGeneratedAccessorsShouldRoundTripOptionalFields() {
        ProviderOnboardingRequest request = new ProviderOnboardingRequest();
        LocalDate dateOfBirth = LocalDate.of(1992, 4, 10);

        request.setGender("female");
        request.setDateOfBirth(dateOfBirth);
        request.setCurrentCity("Noida");
        request.setCurrentState("UP");

        assertThat(request.getGender()).isEqualTo("female");
        assertThat(request.getDateOfBirth()).isEqualTo(dateOfBirth);
        assertThat(request.getCurrentCity()).isEqualTo("Noida");
        assertThat(request.getCurrentState()).isEqualTo("UP");
    }

    @Test
    void detailsResponseShouldDefensivelyCopyListsAndReturnEmptyForNull() {
        ProviderDetailsResponse response = new ProviderDetailsResponse();

        assertThat(response.getServiceAreas()).isEmpty();
        assertThat(response.getDocuments()).isEmpty();

        response.setServiceAreas(null);
        response.setDocuments(null);
        assertThat(response.getServiceAreas()).isEmpty();
        assertThat(response.getDocuments()).isEmpty();

        ProviderDocumentResponse document = new ProviderDocumentResponse();
        document.setId(3L);
        response.setServiceAreas(List.of("Delhi"));
        response.setDocuments(List.of(document));

        List<String> areas = response.getServiceAreas();
        List<ProviderDocumentResponse> documents = response.getDocuments();
        areas.add("Noida");
        documents.clear();

        assertThat(response.getServiceAreas()).containsExactly("Delhi");
        assertThat(response.getDocuments()).extracting(ProviderDocumentResponse::getId).containsExactly(3L);
    }

    @Test
    void pageResponseShouldDefensivelyCopyContentAndHandleNullContent() {
        ProviderListItemResponse item = new ProviderListItemResponse();
        item.setId(1L);
        ProviderPageResponse<ProviderListItemResponse> emptyPage = new ProviderPageResponse<>(null, 0, 0, 0);
        ProviderPageResponse<ProviderListItemResponse> page = new ProviderPageResponse<>(List.of(item), 1, 1, 0);

        assertThat(emptyPage.getContent()).isEmpty();
        List<ProviderListItemResponse> content = page.getContent();
        content.clear();

        assertThat(page.getContent()).extracting(ProviderListItemResponse::getId).containsExactly(1L);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getTotalPages()).isEqualTo(1);
        assertThat(page.getPageNumber()).isZero();
    }

    @Test
    void listItemResponseGeneratedAccessorsShouldRoundTripAllFields() {
        ProviderListItemResponse response = new ProviderListItemResponse();
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 12, 10, 30);

        response.setId(7L);
        response.setProviderCode("PRV1007");
        response.setFullName("Asha");
        response.setEmail("asha@example.com");
        response.setPhoneNumber("9876543210");
        response.setCity("Delhi");
        response.setServiceType("Cleaning");
        response.setExperienceYears(4);
        response.setKycStatus(KycStatus.PENDING);
        response.setOnboardingStatus(OnboardingStatus.PENDING);
        response.setCreatedAt(createdAt);

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getProviderCode()).isEqualTo("PRV1007");
        assertThat(response.getFullName()).isEqualTo("Asha");
        assertThat(response.getEmail()).isEqualTo("asha@example.com");
        assertThat(response.getPhoneNumber()).isEqualTo("9876543210");
        assertThat(response.getCity()).isEqualTo("Delhi");
        assertThat(response.getServiceType()).isEqualTo("Cleaning");
        assertThat(response.getExperienceYears()).isEqualTo(4);
        assertThat(response.getKycStatus()).isEqualTo(KycStatus.PENDING);
        assertThat(response.getOnboardingStatus()).isEqualTo(OnboardingStatus.PENDING);
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
    }
}
