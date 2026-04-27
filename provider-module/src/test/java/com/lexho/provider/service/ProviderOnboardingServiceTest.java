package com.lexho.provider.service;

import com.lexho.catalog.entity.ServiceCategory;
import com.lexho.catalog.repository.CategoryRepository;
import com.lexho.provider.dto.ProviderActionResponse;
import com.lexho.provider.dto.ProviderOnboardingRequest;
import com.lexho.provider.entity.Provider;
import com.lexho.provider.enums.KycStatus;
import com.lexho.provider.enums.OnboardingStatus;
import com.lexho.provider.enums.ProviderDocumentType;
import com.lexho.provider.exception.ProviderOperationException;
import com.lexho.provider.exception.ProviderValidationException;
import com.lexho.provider.mapper.ProviderMapper;
import com.lexho.provider.repository.ProviderDocumentRepository;
import com.lexho.provider.repository.ProviderRepository;
import com.lexho.provider.service.storage.DocumentStorageService;
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
        Provider provider = new Provider();
        provider.setId(1L);
        provider.setOnboardingStatus(OnboardingStatus.APPROVED);
        when(providerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(provider));

        assertThrows(ProviderOperationException.class, () -> providerOnboardingService.approveProvider(1L));
    }

    @Test
    void approveThrowsWhenMandatoryDocumentMissing() {
        Provider provider = new Provider();
        provider.setId(1L);
        provider.setOnboardingStatus(OnboardingStatus.PENDING);
        when(providerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(provider));

        assertThrows(ProviderOperationException.class, () -> providerOnboardingService.approveProvider(1L));
    }

    @Test
    void approveSuccessUpdatesStatusAndKyc() {
        Provider provider = new Provider();
        provider.setId(1L);
        provider.setProviderCode("PRV1001");
        provider.setOnboardingStatus(OnboardingStatus.PENDING);
        provider.setKycStatus(KycStatus.PENDING);

        when(providerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(provider));
        when(providerDocumentRepository.existsByProvider_IdAndDocumentType(1L, ProviderDocumentType.AADHAAR_FRONT)).thenReturn(true);
        when(providerDocumentRepository.existsByProvider_IdAndDocumentType(1L, ProviderDocumentType.AADHAAR_BACK)).thenReturn(true);
        when(providerDocumentRepository.existsByProvider_IdAndDocumentType(1L, ProviderDocumentType.PAN)).thenReturn(true);
        when(providerDocumentRepository.existsByProvider_IdAndDocumentType(1L, ProviderDocumentType.PROFILE_PHOTO)).thenReturn(true);
        when(providerRepository.save(any(Provider.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProviderActionResponse response = providerOnboardingService.approveProvider(1L);

        assertEquals(OnboardingStatus.APPROVED, response.getOnboardingStatus());
        assertEquals(KycStatus.VERIFIED, provider.getKycStatus());
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
