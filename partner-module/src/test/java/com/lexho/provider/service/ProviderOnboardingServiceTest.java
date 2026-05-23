package com.lexho.partner.service;

import com.lexho.catalog.entity.ServiceCategory;
import com.lexho.catalog.repository.CategoryRepository;
import com.lexho.partner.dto.ProviderActionResponse;
import com.lexho.partner.dto.ProviderOnboardingRequest;
import com.lexho.partner.entity.Provider;
import com.lexho.partner.enums.KycStatus;
import com.lexho.partner.enums.OnboardingStatus;
import com.lexho.partner.enums.ProviderDocumentType;
import com.lexho.partner.exception.ProviderOperationException;
import com.lexho.partner.exception.ProviderValidationException;
import com.lexho.partner.mapper.ProviderMapper;
import com.lexho.partner.repository.ProviderDocumentRepository;
import com.lexho.partner.repository.ProviderRepository;
import com.lexho.partner.service.storage.DocumentStorageService;
import com.lexho.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProviderOnboardingServiceTest {

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private ProviderDocumentRepository providerDocumentRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProviderMapper providerMapper;

    @Mock
    private DocumentStorageService documentStorageService;

    @InjectMocks
    private ProviderOnboardingService providerOnboardingService;

    @Test
    void submitOnboardingThrowsWhenDuplicateEmail() {
        ProviderOnboardingRequest request = validRequest();
        when(providerRepository.existsByEmailIgnoreCaseAndDeletedFalse("raju@gmail.com")).thenReturn(true);

        assertThrows(ProviderValidationException.class, () -> providerOnboardingService.submitOnboarding(request));
    }

    @Test
    void approveThrowsWhenProviderNotPending() {
        Provider partner = new Provider();
        partner.setId(1L);
        partner.setOnboardingStatus(OnboardingStatus.APPROVED);
        when(providerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(partner));

        assertThrows(ProviderOperationException.class, () -> providerOnboardingService.approveProvider(1L));
    }

    @Test
    void approveThrowsWhenMandatoryDocumentMissing() {
        Provider partner = new Provider();
        partner.setId(1L);
        partner.setOnboardingStatus(OnboardingStatus.PENDING);
        when(providerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(partner));

        assertThrows(ProviderOperationException.class, () -> providerOnboardingService.approveProvider(1L));
    }

    @Test
    void approveSuccessUpdatesStatusAndKyc() {
        Provider partner = new Provider();
        partner.setId(1L);
        partner.setProviderCode("PRV1001");
        partner.setOnboardingStatus(OnboardingStatus.PENDING);
        partner.setKycStatus(KycStatus.PENDING);

        when(providerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(partner));
        when(providerDocumentRepository.existsByProviderIdAndDocumentType(1L, ProviderDocumentType.AADHAAR_FRONT)).thenReturn(true);
        when(providerDocumentRepository.existsByProviderIdAndDocumentType(1L, ProviderDocumentType.AADHAAR_BACK)).thenReturn(true);
        when(providerDocumentRepository.existsByProviderIdAndDocumentType(1L, ProviderDocumentType.PAN)).thenReturn(true);
        when(providerDocumentRepository.existsByProviderIdAndDocumentType(1L, ProviderDocumentType.PROFILE_PHOTO)).thenReturn(true);
        when(providerRepository.save(any(Provider.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProviderActionResponse response = providerOnboardingService.approveProvider(1L);

        assertEquals(OnboardingStatus.APPROVED, response.getOnboardingStatus());
        assertEquals(KycStatus.VERIFIED, partner.getKycStatus());
    }

    @Test
    void submitOnboardingValidatesCategoryExists() {
        ProviderOnboardingRequest request = validRequest();
        when(providerRepository.existsByEmailIgnoreCaseAndDeletedFalse("raju@gmail.com")).thenReturn(false);
        when(providerRepository.existsByPhoneNumberAndDeletedFalse("9876543210")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ProviderValidationException.class, () -> providerOnboardingService.submitOnboarding(request));
    }

    private ProviderOnboardingRequest validRequest() {
        ProviderOnboardingRequest request = new ProviderOnboardingRequest();
        request.setFullName("Raju");
        request.setEmail("raju@gmail.com");
        request.setPhoneNumber("9876543210");
        request.setServiceCategoryId(1L);
        request.setExperienceYears(5);
        request.setCity("Noida");
        request.setState("UP");
        request.setFullAddress("Sector 62");
        request.setCurrentFullAddress("Sector 104");
        request.setServiceAreas(Set.of("Noida", "Delhi").stream().toList());
        request.setAccountNumber("123456789012");
        request.setIfscCode("SBIN0123456");
        return request;
    }
}
